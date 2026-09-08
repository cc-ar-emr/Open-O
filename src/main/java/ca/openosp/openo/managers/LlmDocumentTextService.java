package ca.openosp.openo.managers;

import ca.openosp.openo.commn.dao.LlmDocumentTextDao;
import ca.openosp.openo.commn.model.Document;
import ca.openosp.openo.commn.model.LlmDocumentText;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.PDFGenerationException;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles PDF text extraction for LLM patient exports with two layers of caching:
 *
 *   Layer 1 — In-memory ConcurrentHashMap keyed by document_no.
 *             Fast path: no DB round-trip if already loaded this JVM session.
 *             Bounded to MAX_CACHE_SIZE entries via a simple eviction strategy.
 *
 *   Layer 2 — llm_document_text DB table (persistent across restarts).
 *             Hit: return stored text if SHA-256 hash of current PDF bytes matches.
 *             Miss / hash mismatch: re-extract, persist new record, update L1.
 *
 * This means OCR/extraction runs at most once per unique PDF version.
 *
 * Thread safety: ConcurrentHashMap handles concurrent reads. Extraction is
 * intentionally not synchronised per document_no — a small risk of duplicate
 * extraction on first load is acceptable and far simpler than per-key locking.
 */
@Service
public class LlmDocumentTextService {

    private static final Logger logger = MiscUtils.getLogger();

    /** Cap the in-memory cache to avoid unbounded heap growth on busy servers. */
    private static final int MAX_CACHE_SIZE = 500;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private LlmDocumentTextDao llmDocumentTextDao;

    /**
     * L1 in-memory cache: document_no → extracted text (null = extraction failed / no text layer).
     * We store null explicitly so we don't re-attempt extraction every call for image-only PDFs.
     */
    private final ConcurrentHashMap<Integer, String> memoryCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns extracted plain text for the given PDF document, using cached
     * results where possible.
     *
     * @param loggedInInfo  OSCAR session context
     * @param document      Document metadata record (must be a PDF)
     * @param demographicNo patient demographic number (used to populate the DB record)
     * @return extracted text, or null if the PDF has no text layer or extraction failed
     */
    public String getExtractedText(LoggedInInfo loggedInInfo, Document document, Integer demographicNo) {
        Integer docNo = document.getDocumentNo();

        // L1: in-memory hit
        if (memoryCache.containsKey(docNo)) {
            return memoryCache.get(docNo);
        }

        // Load PDF bytes — needed for hash comparison and possibly extraction
        byte[] pdfBytes = loadPdfBytes(loggedInInfo, docNo);
        if (pdfBytes == null) {
            // Can't read the file at all — don't cache, allow retry next call
            return null;
        }

        String currentHash = sha256Hex(pdfBytes);

        // L2: DB hit — check hash to confirm PDF hasn't changed
        Optional<LlmDocumentText> cached = llmDocumentTextDao.findByDocumentNo(docNo);
        if (cached.isPresent() && currentHash.equals(cached.get().getContentHash())) {
            String text = cached.get().getExtractedText();
            putInMemoryCache(docNo, text);
            return text;
        }

        // Cache miss or stale (PDF was re-uploaded) — extract and persist
        String extractedText = extractTextFromBytes(pdfBytes);
        persist(docNo, demographicNo, extractedText, currentHash, cached);
        putInMemoryCache(docNo, extractedText);
        return extractedText;
    }

    /**
     * Bulk-load cached texts for all PDF documents belonging to a demographic.
     * Returns a map of document_no → extracted text for fast lookup in LLMExtractorService.
     *
     * Documents not yet in the cache are NOT extracted here — extraction is lazy,
     * triggered by getExtractedText() per document.
     */
    public Map<Integer, String> getPreloadedTexts(Integer demographicNo) {
        List<LlmDocumentText> rows = llmDocumentTextDao.findByDemographicNo(demographicNo);
        if (rows.isEmpty()) return Collections.emptyMap();

        Map<Integer, String> result = new LinkedHashMap<>();
        for (LlmDocumentText row : rows) {
            result.put(row.getDocumentNo(), row.getExtractedText());
            // Warm up L1 while we're here
            putInMemoryCache(row.getDocumentNo(), row.getExtractedText());
        }
        return result;
    }

    /**
     * Evict a document from both cache layers — call this when a document is deleted
     * or replaced so stale text is not served.
     */
    public void evict(Integer documentNo) {
        memoryCache.remove(documentNo);
        llmDocumentTextDao.deleteByDocumentNo(documentNo);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private byte[] loadPdfBytes(LoggedInInfo loggedInInfo, Integer docNo) {
        try {
            Path pdfPath = documentManager.renderDocument(loggedInInfo, docNo.toString());
            if (pdfPath == null || !Files.exists(pdfPath)) {
                logger.warn("renderDocument returned no path for document {}", docNo);
                return null;
            }
            return Files.readAllBytes(pdfPath);
        } catch (PDFGenerationException | IOException e) {
            logger.warn("Could not load PDF bytes for document {}", docNo, e);
            return null;
        }
    }

    private String extractTextFromBytes(byte[] pdfBytes) {
        try (PDDocument pdf = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf).trim();
            return text.isEmpty() ? null : text;
        } catch (IOException e) {
            logger.warn("PDFBox text extraction failed", e);
            return null;
        }
    }

    private void persist(Integer docNo, Integer demographicNo,
                         String text, String hash,
                         Optional<LlmDocumentText> existing) {
        try {
            LlmDocumentText record = existing.orElseGet(() ->
                new LlmDocumentText(docNo, demographicNo, null, hash));
            record.setExtractedText(text);
            record.setContentHash(hash);
            record.setExtractedAt(new Date());
            llmDocumentTextDao.save(record);
        } catch (Exception e) {
            // Persistence failure is non-fatal — the caller still gets the text,
            // it just won't be cached for next time
            logger.error("Failed to persist extracted text for document {}", docNo, e);
        }
    }

    private void putInMemoryCache(Integer docNo, String text) {
        if (memoryCache.size() >= MAX_CACHE_SIZE) {
            // Simple eviction: remove the first key we find
            // For production, consider Caffeine/Guava with LRU + TTL
            Integer oldest = memoryCache.keys().nextElement();
            memoryCache.remove(oldest);
        }
        // ConcurrentHashMap doesn't allow null values — use sentinel
        memoryCache.put(docNo, text != null ? text : "");
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present in all JVMs
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
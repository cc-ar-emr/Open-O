//--> Document Ready Methods 
jQuery(document).ready( function() {

	//--> Autocomplete searches
	jQuery( "#searchHealthCareTeamInput" ).autocomplete({				
		source: function( request, response ) {  
			var role = jQuery('#selectHealthCareTeamRoleType option:selected').text().trim();
			var url;
			if( "PHARMACY" == role ) {
				url = ctx + "/oscarRx/managePharmacy.do?method=search";
			} else {
				url = ctx + "/demographic/Contact.do?method=searchAllContacts&searchMode=search_name&orderBy=c.lastName,c.firstName";
			}

			jQuery.ajax({
				url: url,
				type: "GET",
				dataType: "json",
				data: {
				 	term: request.term   			  
				},
				contentType: "application/json",
				success: function( data ) {
					response(jQuery.map(data, function( item ) {
						if( role == "PHARMACY" ) {
							 return {
							  label: item.name + " :: " 
							  + item.address 
							  + " :: " + item.city,
							  value: item.id,
							  pharmacy: item
							 }
						} else {
							return {
							  label: item.lastName + ", " 
							  + item.firstName + " :: "
							  + item.residencePhone
							  + " :: " + item.address 
							  + " " + item.city,
							  value: item.id,
							  contact: item
							 }
						}
						 
					}));
				}
			});
	    },
	    minLength: 2,  
	    focus: function( event, ui ) {
	    	event.preventDefault();
	        return false;
	    },
	    select: function(event, ui) { 
	    	event.preventDefault(); 
			if( ui.item.pharmacy ) {
				linkPharmacy( ui.item.pharmacy );				
			} else {
				linkExternalProvider( ui.item.contact );		
			}
	    }      
	});
})
# Open-OSP EMR DEVCONTAINER Environment Setup

This document outlines the steps for setting up your development environment for the Open-OSP EMR project using Docker
and VS-Code.

## Prerequisites

•⁠  ⁠*Docker Desktop:* Installed and running
•⁠  ⁠*VS Code:* Installed with the "Dev Containers" extension
    * Install "Dev Containers" Extension: Open VS Code, click on the Extensions icon (four squares in the left sidebar),
      search for "Dev Containers" by Microsoft and click "Install".
•⁠  ⁠*Git:* Installed

## Steps

1.⁠ ⁠*Clone the repository if not already done:*
   ⁠ bash
   git clone https://github.com/MagentaHealth/open-osp.git
   cd open-osp
    ⁠

2.⁠ ⁠*Confirm port availability:*
   Before proceeding, ensure that no other processes (Tomcat, MySQL) are using ports 8080 and 3306 on your PC/Macbook.
   Also, ensure that no other docker containers are running on those ports.

3.⁠ ⁠*Open the project in VS Code:*
    * Open VS Code and navigate to the project directory.
    * VS Code should automatically detect the ⁠ .devcontainer ⁠ folder and prompt you to "Reopen in Container".
    * Click "Reopen in Container" to start the development environment.
    * Note - In case "Reopen in Container" option does not work then:
        * Look on bottom left of VSCode you will find a remote connection icon (green colored). Click on it.
        * It will prompt few option, select "Reopen in Container".
        
💡 Important: Stay logged out of Docker Desktop while performing these steps.

4.⁠ ⁠Once it Reopen in Container, you need to wait until it finishes the setup process. This process will:
    * Build the Docker images for the Open-OSP application and database.
    * Configure the images and start the containers.
      You will see the extensions in the left sidebar along with the "Workspace" folder.
      You will also see the Java extension starts processing the project.

### Initial Compilation

Let's now compile Oscar.

This might take absolutely forever when doing it for the first time.

Once you've built it for the first time, a subsequent full build should take about 2 minutes on MacOS and about 8
minutes on Windows, as we'll have managed to cache a bunch of Maven compilation artifacts.


   ⁠ zsh
   make
    ⁠

* If you encounter an error like:  wrong integrity and version *
Run the following Maven command inside the Dev Container: ⁠ mvn se.vandmo:dependency-lock-maven-plugin:1.1.0:lock ⁠
(in windows it would take more than 1 hour to build)


Once the compilation is successful, a ⁠ target/oscar ⁠ directory full of artifacts will be created. This is a so-called "
Exploded WAR".

## Access the application:

    * Open your web browser and navigate to ⁠ http://localhost:8080 ⁠.
    * You should see the Open-OSP EMR application running.

### Subsequent Compilations

For developers who are not compiling for the first time in the dev container, it is recommended to clean the previous build artifacts before compiling again. This ensures a fresh build environment.

   ```zsh
   make clean
   make install
   ```

### View Oscar error logs

Currently, oscar error logs are sent to console without saving to a log file. If you need to read oscar error logs from dev container, can use this command to see real-time logs.

   ```zsh
   cat /tomcat.pid | xargs -I {} tail -f /proc/{}/fd/1
   ```

## Additional Notes

•⁠  ⁠The ⁠ .devcontainer/development/config/shared/local.env ⁠ file contains environment variables that can be customized for
  your development environment.
•⁠  ⁠You can find more information about the Open-OSP EMR project and its development environment in the project's
  documentation.

## Files Included in the Dev-Container Environment

•⁠  ⁠*⁠ .devcontainer/devcontainer.json ⁠:* Defines the configuration for the development environment, including Docker
  images to use, ports to forward, and user settings.
•⁠  ⁠*⁠ .devcontainer/development/Dockerfile ⁠:* Defines the Docker image for the Open-OSP application.
•⁠  ⁠*⁠ .devcontainer/db/Dockerfile ⁠:* Defines the Docker image for the Open-OSP database.
•⁠  ⁠*⁠ .devcontainer/development/setup/setup.sh ⁠:* Automates the setup process for the development environment.
•⁠  ⁠*⁠ .devcontainer/docker-compose.yml ⁠:* Defines the Docker Compose configuration for the development environment,
  including the services to run and their dependencies.

## Additional Resources

•⁠  ⁠*Docker Desktop:* https://www.docker.com/products/docker-desktop
•⁠  ⁠*Dev Containers Extension:* https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers
•⁠  ⁠*VS Code Developing with Dev Containers:* https://code.visualstudio.com/docs/devcontainers/containers
•⁠  ⁠*Docker Desktop Guide: Getting Started:* https://docs.docker.com/desktop
•⁠  ⁠*Docker Compose:* https://docs.docker.com/compose**
•⁠  ⁠*Dockerfile:* https://docs.docker.com/reference/dockerfile/

## Opening the Dev-Container Environment in VS Code through WSL

* **Note:** Make sure you have the Prerequisites installed and configured. 

### Steps

1. **Go to the command line and type wsl**
    ```bash 
    wsl
    ```
2. **Create a directory**
    ```bash
    mkdir softsec
    ```
3. **Navigate to the softsec dir**
    ```bash
    cd softsec
    ```
4. **Clone the GitHub repo Open-O**
    ```bash
    git clone https://github.com/cc-ar-emr/Open-O.git
    ```
5. **Navigate to the Open-O repo dir**
    ```bash
    cd Open-O
    ```
6. **Start VS Code**
    ```bash
    code ./
    ```

* This should open the Open-O repo in VS Code and prompt you to reopen the folder in the dev container environment.

## Enjoy developing with Open-OSP!

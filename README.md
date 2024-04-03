<p align="center">
    <img src="https://i.ibb.co/gRgtpXF/logo.png"  alt="OGXRepacker title"/>
</p>

<p align="center">
    <a href="https://github.com/astarivi/OGXRepacker/releases"><img src="https://img.shields.io/badge/download-36222c?logoColor=white&style=for-the-badge" alt="Download badge"></a>
</p>

OGXRepacker is a general purpose, performance first, simplistic XISO processing software for Original Xbox games.
It specializes in converting game images to XISO images, while also offering the option to create an attacher.

Don't know what any of that means?, leave the settings on its defaults, select an input and output folder, and press
"Process" to get started.

# Quickstart
Download the latest OGXRepacker release.

Download the latest [self executing package](https://github.com/astarivi/OGXRepacker/releases/latest/download/OGXRepacker.jar) 
and run it with Java 21, like this; `java -jar OGXRepacker.jar`.
Double-clicking it works too, but only if your system has Java 21 set as the default file association for .jar files.

# Features

- Cross-platform
- Maximum input image compatibility
- Simple and intuitive user interface
- Able to split output images on the fly
- Always rebuilds and strips input images, producing smaller output images
- Includes all known attachers, from Cerbios to DriveImageUtils
- Leverages Repackinator content database, but isn't limited to it

# Configuration

Every feature available in OGXRepacker is listed here.

- ### Naming Convention
    - OGXRepacker: Uses the OGXRepacker filename system, consisting of the Repackinator title, but uses a single 
    letter for the region.
    - Keep filename: Keeps the input filename, and uses it for the output.
- ### Create Attacher
    - Cerbios: Creates a Cerbios attacher, compatible with CCI images (although CCI isn't yet supported)
    - Stellar: Creates a Stellar attacher, compatible with CSO images
    - DriveImageUtils (Legacy): Creates a DriveImageUtils attacher, compatible with XISO images and old softmods.
    - None: Skips the attacher, and produces only output images.
- ### Pack Mode
    - XDVDFS (XISO): Packs the image as a XDVDFS compliant image
- ### Split Image
    - Split in half: Splits the output image in two parts of approximately the same size.
    - Do not split: Produces a single .iso output image.

# Preview
<img src="https://i.ibb.co/h9MWXzH/Screenshot-2024-03-18-221926.png" />

# System Requirements
- OS: Windows, Linux
- Architecture: x64, x86, ARM64, ARM
- Display: A graphical system for display
- Java >17 (Java 21 recommended)

# Planned Features

- macOS support (this is theoretically supported, but I lack a Mac system to build with)
- More image splitting options
- Extract the .tbn image from the game default.xbe
- CSO support
- CCI support
- Command line support

# Credits

- [Repackinator](https://github.com/Team-Resurgent/Repackinator) and Team Resurgent for the `RepackList.json` database.
- Cerbios Team, for their attacher bundled inside OGXRepacker as a binary.
- [stellar-attach](https://github.com/MakeMHz/stellar-attach) bundled inside OGXRepacker as a binary.
- Rmenhal, for the DriveImageUtils attacher, bundled inside OGXRepacker as a binary.
- [xdvdfs](https://github.com/antangelo/xdvdfs) for the amazing XDVDFS image library.
- [splitfile-rs](https://github.com/alemigo/splitfile-rs) used for output image splitting
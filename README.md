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

- Cross-platform, multi-arch
- Maximum input image compatibility
- Simple and intuitive user interface
- Able to split output images on the fly
- Able to rebuild, trim, keep or extract the input image
- Includes all known attachers, from Cerbios to DriveImageUtils
- Leverages Repackinator content database

# Configuration

Every feature available in OGXRepacker is listed here.

- ## Naming Convention
    - ### OGXRepacker
      - Uses the OGXRepacker filename system, consisting of the Repackinator title, but uses a single letter for the region.
      - Example: `Halo 2 (G)`
    - ### Repackinator
      - Uses the Repackinator naming convention, three letter region codes.
      - Example: `Halo 2 (GLO)`
    - ### Keep filename
      - Keeps the input filename, and uses it for the output.
      - Example: `HALO 2`

- ## Pack Mode
  - ### XISO Auto
    - Automatically chooses the best `XISO` packing mode for the detected input files.
    - Will always try to produce the smallest output image possible.
    - Will default to `XISO Rebuild` if **Naming Convention** is set to `Keep filename`
  - ### XISO Rebuild
    - Rebuilds the input image, or packs an input folder.
    - Often produces smaller images with no performance penalties.
    - A few games are known to fail to boot when this mode is used. Use `XISO Auto`, which checks for these cases.
  - ### XISO Trim
    - Trims the empty sectors at the end of the image, leading to modest file size savings.
    - Redump images are the only known input that benefits from this mode.
    - If the input is detected to be a folder, this mode defaults to `XISO Rebuild`.
  - ### XISO
    - Keeps the input image intact, and passes it through.
    - If a Redump image is used, this mode will strip unnecessary data from the image, but will keep the XISO partition untouched.
    - If the input is detected to be a folder, this mode defaults to `XISO Rebuild`.
  - ### Extract only
    - Extracts the contents of an input image.
    - Only `.iso` images are supported.
    - If input is a folder, it will be skipped.
    - Incompatible with Attachers and Split modes.

- ## Create Attacher
    - ### Cerbios
      - Creates a Cerbios attacher.
      - Supports CCI images.
      - No support for CSO images.
      - Best overall compatibility.
    - ### Stellar 
      - Creates a Project Stellar attacher.
      - Supports CSO images.
      - No support for CCI images.
    - ### DriveImageUtils (Legacy) 
      - Creates a DriveImageUtils attacher, compatible with old softmods.
      - No support for CCI images.
      - No support for CSO images.
      - Most consoles support newer attachers, use only if needed.
    - ### None
      - Skips the attacher, and produces only output images.

Note: All attachers support regular XISO images.

- ## Split Image
    - ### Split at FATX limit
      - Splits the output image only when it's close to the FATX size limit.
    - ### Split in half
      - Splits the output image in two parts of approximately the same size.
      - If input file is too small, only a single file will be produced.
    - ### Do not split
      - Produces a single `.iso` output image.

# Preview
<img src="https://i.ibb.co/h9MWXzH/Screenshot-2024-03-18-221926.png" />

# System Requirements
- OS: Windows, Linux
- Architecture: x64, x86, ARM64, ARM, RISCV
- Display: A graphical system for display
- Java >17 (Java 21 recommended)

# Planned Features

- macOS support (this is theoretically supported, but I lack a Mac system to build with)
- Extraction of the .tbn image from the game default.xbe
- CSO support
- CCI support
- Theme support

# Credits

- [Repackinator](https://github.com/Team-Resurgent/Repackinator) and Team Resurgent for the `RepackList.json` database.
- Cerbios Team, for their attacher bundled inside OGXRepacker as a binary.
- [stellar-attach](https://github.com/MakeMHz/stellar-attach) bundled inside OGXRepacker as a binary.
- Rmenhal, for the DriveImageUtils attacher, bundled inside OGXRepacker as a binary.
- [xdvdfs](https://github.com/antangelo/xdvdfs) for the amazing XDVDFS image library.
- [splitfile-rs](https://github.com/alemigo/splitfile-rs) used for output image splitting
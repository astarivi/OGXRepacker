<p align="center">
    <img src="https://i.ibb.co/gRgtpXF/logo.png" alt="OGXRepacker title"/>
</p>

<p align="center">
    <a href="https://github.com/astarivi/OGXRepacker/releases"><img src="https://img.shields.io/badge/download-36222c?logoColor=white&style=for-the-badge" alt="Download badge"></a>
</p>

OGXRepacker is a general-purpose, simplistic XISO processing software for Original Xbox games. It specializes in converting game images to XISO images while also offering the option to create an attacher.

If you’re unsure what this means, simply leave the settings at their defaults, select an input and output folder, and press "Process" to get started.

# Quickstart

## Windows
Windows-specific executables are available. Windows on ARM is **not** supported. Both 64-bit and 32-bit architectures are fully supported.

- ### The easiest way to get started is to download the latest [OGX Windows installer](https://github.com/astarivi/OGXRepacker/releases/latest/download/OGXRepacker_x64_Setup.exe). This includes everything you will need to run OGX.
- The [bundled Windows portable](https://github.com/astarivi/OGXRepacker/releases/latest/download/ogx-windows-bundled.zip) distribution contains everything the installable distribution does, in a portable package.
- The [minimal Windows portable](https://github.com/astarivi/OGXRepacker/releases/latest/download/ogx-windows-minimal.zip) distribution only has the essentials, and requires Java >17 to be installed in your system.

## Linux
Linux has its own distributable package, which includes a wrapper script for launch.

```bash
# Download the latest `ogx-VERSION.zip` release.
# Remember to replace VERSION with the latest release version.
wget https://github.com/astarivi/OGXRepacker/releases/latest/download/ogx-VERSION.zip

# Extract it
unzip ogx-VERSION.zip
cd ogx-VERSION/bin

# Make the script executable
chmod +x ogx
# Run the script
./ogx
```

## Universal
This release works on both Windows and Linux. Use this if you experience problems with your platform-specific release.

- Download the latest `ogx-VERSION-all.jar` release.
- Run with `java -jar ogx-VERSION-all.jar`
- If Java version 17 or higher is associated with .jar files, double-clicking works too.

# Features

- Cross-platform, multi-arch
- Maximum compatibility with input images
- Simple and intuitive user interface
- Able to split output images on the fly
- Able to rebuild, trim, keep, or extract the input image
- Includes all known attachers, from Cerbios to DriveImageUtils
- Leverages Repackinator content database

# Configuration

Every feature available in OGXRepacker is listed here.

- ## Naming Convention
    - ### OGXRepacker
        - Uses the OGXRepacker filename system, consisting of the Repackinator title and a single-letter region code.
        - Example: `Halo 2 (G)`
    - ### Repackinator
        - Uses the Repackinator naming convention, with three-letter region codes.
        - Example: `Halo 2 (GLO)`
    - ### Keep filename
        - Keeps the input filename and uses it for the output.
        - Example: `HALO 2`

- ## Pack Mode
    - ### XISO Auto
        - Automatically chooses the best `XISO` packing mode for the detected input files.
        - Will always try to produce the smallest output image possible.
        - Defaults to `XISO Rebuild` if **Naming Convention** is set to `Keep filename`.
    - ### XISO Rebuild
        - Rebuilds the input image or packs an input folder.
        - Often produces smaller images with no performance penalties.
        - A few games are known to fail to boot when this mode is used. Use `XISO Auto`, which checks for these cases.
    - ### XISO Trim
        - Trims the empty sectors at the end of the image, leading to modest file size savings.
        - Redump images are the only known input that benefit from this mode.
        - Defaults to `XISO Rebuild` if the input is detected to be a folder.
    - ### XISO
        - Keeps the input image intact and passes it through.
        - If a Redump image is used, this mode will strip unnecessary data from the image but will keep the XISO partition untouched.
        - Defaults to `XISO Rebuild` if the input is detected to be a folder.
    - ### Extract only
        - Extracts the contents of an input image.
        - Only `.iso` images are supported.
        - If input is a folder, it will be skipped.
        - Incompatible with Attachers and Split modes.

- ## Create Attacher
    - ### Cerbios
        - Creates a Cerbios attacher.
        - Supports CCI images.
        - Does not support CSO images.
        - Best overall compatibility.
    - ### Stellar
        - Creates a Project Stellar attacher.
        - Supports CSO images.
        - Does not support CCI images.
    - ### DriveImageUtils (Legacy)
        - Creates a DriveImageUtils attacher, compatible with old softmods.
        - Does not support CCI images.
        - Does not support CSO images.
        - Most consoles support newer attachers; use only if needed.
    - ### None
        - Skips the attacher and produces only output images.

Note: All attachers support regular XISO images.

- ## Split Image
    - ### Split at FATX limit
        - Splits the output image only when it's close to the FATX size limit.
    - ### Split in half
        - Splits the output image into two parts of approximately the same size.
        - If the input file is too small, only a single file will be produced.
    - ### Do not split
        - Produces a single `.iso` output image.

# Preview
<img src="https://i.ibb.co/y4NLGLj/image.png" />

# Supported Systems
- OS: Windows (XP, Vista, 7, 8, 10 & 11), Linux (modern glibc)
- Architecture: x64, x86, ARM64, ARM, RISCV
- Display: A graphical system for display (this is a graphical tool)
- Java: 17 or higher

# Planned Features

- macOS support (this is theoretically supported, but I currently lack access to a Mac system to build with)
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
- [splitfile-rs](https://github.com/alemigo/splitfile-rs) used for output image splitting.
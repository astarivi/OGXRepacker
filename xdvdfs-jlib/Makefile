.PHONY: all, build, clean

build:
	# x64 targets
	cross build --release --target x86_64-pc-windows-gnu
	cross build --release --target x86_64-unknown-linux-gnu
	# x86 targets
	cross build --release --target i686-pc-windows-gnu
	cross build --release --target i686-unknown-linux-gnu
	# arm64
	cross build --release --target aarch64-unknown-linux-gnu
	# arm
	cross build --release --target arm-unknown-linux-gnueabi
	# riscv
	cross build --release --target riscv64gc-unknown-linux-gnu

clean:
	cargo clean
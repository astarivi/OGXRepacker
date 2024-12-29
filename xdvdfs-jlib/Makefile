.PHONY: all, build, clean

generic-cross-build:
	cargo clean
	cross build --release --target $(TARGET)
	cp -r target/$(TARGET) out/

build:
	mkdir out
	# x64 targets
	$(MAKE) generic-cross-build TARGET=x86_64-pc-windows-gnu
	$(MAKE) generic-cross-build TARGET=x86_64-unknown-linux-gnu
	# x86 targets
	$(MAKE) generic-cross-build TARGET=i686-pc-windows-gnu
	$(MAKE) generic-cross-build TARGET=i686-unknown-linux-gnu
	# arm64
	$(MAKE) generic-cross-build TARGET=aarch64-unknown-linux-gnu
	# arm
	$(MAKE) generic-cross-build TARGET=arm-unknown-linux-gnueabi
	# riscv
	$(MAKE) generic-cross-build TARGET=riscv64gc-unknown-linux-gnu

clean:
	rm -rf out
	cargo clean
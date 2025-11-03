# 🚀 Chipyard + Gemmini + VC707 + FireMarshal Integration Guide

This document captures all the steps followed to:

- Set up Chipyard with Gemmini  
- RISCV Cross Compilation of Leela Chess Zero  
- Build the VC707 FPGA bitstream  
- Build and test Linux workloads using FireMarshal  
- Plan for Linux boot on FPGA and ONNX Runtime [Gemmini Version] cross-compilation (pending)

---

## 🧱 1. System Setup (Ubuntu 22.04)

```bash
sudo apt-get update
sudo add-apt-repository -y ppa:git-core/ppa
sudo apt install -y git
sudo apt-get install kmod
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh
```
## ⚙️ 2. Chipyard Setup

```bash
cd chipyard
./scripts/build-setup.sh
conda tos accept --override-channels --channel https://repo.anaconda.com/pkgs/main
conda tos accept --override-channels --channel https://repo.anaconda.com/pkgs/r
git submodule update --init generators/gemmini/
source env.sh
```

---
# ====================================================================
# 2. Setup RISC-V toolchain, install zlib, and prepare LC0 build
# ====================================================================

```bash
# RISC-V cross compiler
export CC=riscv64-unknown-linux-gnu-gcc
export CXX=riscv64-unknown-linux-gnu-g++
export AR=riscv64-unknown-linux-gnu-ar
export STRIP=riscv64-unknown-linux-gnu-strip
export RANLIB=riscv64-unknown-linux-gnu-ranlib
```

### ============================================================
### Step 1: Download and build zlib for RISC-V
### ============================================================
```bash
wget https://zlib.net/zlib-1.3.1.tar.gz
tar xvf zlib-1.3.1.tar.gz
cd zlib-1.3.1
export CFLAGS="-O3 -fPIC"
mkdir build && cd build
../configure --prefix=$PWD/install
make -j$(nproc)
make install
```
### ============================================================
### Step 2: Set environment variables for LC0 build
### ============================================================

```bash

export ZLIB_ROOT=$PWD/install
export LD_LIBRARY_PATH=$ZLIB_ROOT/lib:$LD_LIBRARY_PATH
export PKG_CONFIG_PATH=$ZLIB_ROOT/lib/pkgconfig:$PKG_CONFIG_PATH
# Optional: add zlib includes for compile
export CFLAGS="-I$ZLIB_ROOT/include"
export CXXFLAGS="-I$ZLIB_ROOT/include"
export LDFLAGS="-L$ZLIB_ROOT/lib -lz"
# Verify zlib files exist
ls $ZLIB_ROOT/include/zlib.h
ls $ZLIB_ROOT/lib/libz.a
```
### ============================================================
### Step 3: Build LC0 for RISC-V
### ============================================================
```bash
# Remove previous build directory
rm -rf build_riscv
# Setup Meson build
meson setup build_riscv --cross-file=../riscv64.txt 
# Build with Ninja
ninja -C build_riscv -v
# Verify build output
ls build_riscv/
cd ../../
```



## 🧩 4. Building a Gemmini based Bitstream for VC707

```bash
cp ../VC707GemminiConfig.scala fpga/src/main/scala/vc707/
make SUB_PROJECT=vc707 CONFIG=VC707GemminiConfig bitstream  # (Vivado with VC707 Board Files)
cd ..
```

---

## 🧰 5. FireMarshal Setup

```bash
cd FireMarshal
./init-submodules.sh
export PATH=$PATH:$(pwd)
marshal -h
```

---

## 🧪 6. Build the Workload

```bash
cp ../br-base.json images/firechip/br-base/
marshal build br-base.json
```

---

## 🧱 7. Image Directory and QEMU Execution

```bash
cd images/firechip/br-base
qemu-system-riscv64 -machine virt -m 4G -nographic -bios none -kernel br-base-bin -drive file=br-base.img,format=raw,id=hd0 -device virtio-blk-device,drive=hd0
```

---

## 📝 Notes

- Your LC0 binary will appear inside QEMU at `/root/lc0`  
- Your weights file will appear at `/root/t1-256x10-distilled-swa-2432500.pb.gz`

---

## ⚠️ Partially Completed (Due to Issues)

Followed these steps for VCU118 to boot for VC707 (Default Config):  
[https://chipyard.readthedocs.io/en/stable/Prototyping/VCU118.html#running-linux-on-vcu118-designs](https://chipyard.readthedocs.io/en/stable/Prototyping/VCU118.html#running-linux-on-vcu118-designs)

This was planned to be a Linux baseline. However, the boot stopped with OpenSBI and did not continue. I tried to fix this issue by multiple ways but nothing worked so far. 

The plan was to boot Linux in the default VC707Config and, if it works, replace the `br-base.json` under prototype directory (as given in the above directory) with the above (to include binaries) and rebuild the Linux image using the above steps and run it on the VC707GemminiConfig bitstream generated above.

After it works, the ONNX Runtime version given to Gemmini should have been used to cross-compile Gemmini and both the executables should have been compiled and compared.


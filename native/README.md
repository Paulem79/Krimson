# Krimson Native Library

This directory contains the Rust implementation of performance-critical functions for the Krimson plugin, exposed via JNI (Java Native Interface).

## Building

To build the native library, you need to have Rust installed. If you don't have it, install it from [rustup.rs](https://rustup.rs/).

### Build Commands

```bash
# Build release version (optimized)
cargo build --release

# Build debug version (for development)
cargo build
```

### Output Location

After building, the compiled library will be located at:
- **Linux**: `target/release/libkrimson_native.so`
- **Windows**: `target/release/krimson_native.dll`
- **macOS**: `target/release/libkrimson_native.dylib`

## Installation

Copy the compiled library to a location where Java can find it:
1. Place it in the plugin's directory, or
2. Add its location to the `java.library.path` system property

## Functions

### `parseBlockKey`

Parses a block key string in the format `x{num}y{num}z{num}` and returns an array of three integers `[x, y, z]`.

This function provides significant performance improvements over regex-based parsing, especially during chunk loading events where it's called frequently.

**Input format**: `x(\d+)y(-?\d+)z(\d+)`
- Example: `x5y64z10` → `[5, 64, 10]`
- Example: `x0y-64z15` → `[0, -64, 15]`

**Returns**: `int[3]` containing `[x, y, z]` coordinates, or `null` if parsing fails.

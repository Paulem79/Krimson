# Rust JNI Integration for Key Parsing Optimization

## Overview

This PR integrates Rust via JNI (Java Native Interface) to optimize the CPU-intensive regex-based key parsing operation in `PersistentDataUtils.getBlockFromKey()`. The parsing function is called frequently during chunk loading events, making it a performance bottleneck.

## Implementation

### 1. Rust Native Library (`native/`)

The Rust implementation provides a fast, string-slicing based parser for the coordinate format `x{num}y{num}z{num}`:

- **Location**: `native/src/lib.rs`
- **Function**: `Java_ovh_paulem_krimson_utils_NativeUtil_parseBlockKey`
- **Performance**: Uses simple string slicing without regex overhead
- **Format**: `x(\d+)y(-?\d+)z(\d+)` → `[x, y, z]`

#### Building the Native Library

```bash
cd native
cargo build --release
```

The compiled library will be located at:
- **Linux**: `native/target/release/libkrimson_native.so`
- **Windows**: `native/target/release/krimson_native.dll`
- **macOS**: `native/target/release/libkrimson_native.dylib`

### 2. Java Integration (`NativeUtil.java`)

The `NativeUtil` class handles:
- Native library loading with fallback mechanism
- JNI method declaration for `parseBlockKey(String key)`
- Status checking via `isLoaded()` method

**Features:**
- Attempts to load from `java.library.path`
- Falls back to extracting from plugin resources if available
- Gracefully handles missing native library

### 3. PersistentDataUtils Integration

The `getBlockFromKey()` method now:
1. Checks if native library is loaded via `NativeUtil.isLoaded()`
2. Uses `NativeUtil.parseBlockKey()` if available (fast path)
3. Falls back to regex implementation if native library is missing (compatibility)

## Performance Benefits

The Rust implementation provides significant performance improvements:
- **No regex compilation overhead** - uses simple string operations
- **Zero-copy parsing** - works directly with string slices
- **Native code speed** - compiled to machine code vs. JVM bytecode
- **Reduced GC pressure** - minimal object allocations

This is especially important during chunk loading where this function may be called hundreds of times in quick succession.

## Backwards Compatibility

The implementation maintains full backwards compatibility:
- **Fallback mechanism**: If the native library isn't available, the original regex-based implementation is used
- **No breaking changes**: The public API of `PersistentDataUtils` remains unchanged
- **Optional optimization**: The plugin works with or without the native library

## Deployment

### Option 1: Bundle Native Library
Place the compiled `.so`/`.dll`/`.dylib` file in the plugin's resources under `/native/` directory. The `NativeUtil` class will automatically extract and load it.

### Option 2: System Library Path
Copy the compiled library to a directory in `java.library.path` (e.g., `/usr/lib` on Linux).

### Option 3: No Native Library
Simply deploy without the native library. The plugin will automatically fall back to the regex implementation and log a warning.

## Testing

Run the included tests to verify parsing behavior:

```bash
# Test Rust implementation
cd native && cargo test

# Test Java fallback
javac -d /tmp/test src/test/java/ovh/paulem/krimson/utils/FallbackParsingTest.java
java -cp /tmp/test ovh.paulem.krimson.utils.FallbackParsingTest
```

## Files Changed

- `native/Cargo.toml` - Rust project configuration
- `native/src/lib.rs` - Rust JNI implementation
- `native/README.md` - Build instructions for native library
- `src/main/java/ovh/paulem/krimson/utils/NativeUtil.java` - JNI wrapper class
- `src/main/java/ovh/paulem/krimson/utils/PersistentDataUtils.java` - Updated to use native parsing
- `src/test/java/ovh/paulem/krimson/utils/FallbackParsingTest.java` - Test for fallback implementation

## Security Considerations

- The native library is loaded from trusted sources only (plugin resources or system library path)
- Input validation is performed in both Rust and Java layers
- No external dependencies in the Rust code beyond the `jni` crate
- The fallback ensures functionality even if native library loading fails

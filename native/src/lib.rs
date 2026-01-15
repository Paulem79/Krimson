use jni::JNIEnv;
use jni::objects::{JClass, JString, JByteArray};
use jni::sys::{jintArray, jbyteArray};
use flate2::Compression;
use flate2::write::{DeflateEncoder, DeflateDecoder};
use std::io::Write;

/// Parses a block key string in the format "x{num}y{num}z{num}" into [x, y, z] coordinates.
/// 
/// This is a JNI function exposed to Java as `NativeUtil.parseBlockKey(String)`.
/// It provides a high-performance alternative to regex-based parsing by using
/// simple string slicing operations.
/// 
/// # Format
/// Expected format: `x(\d+)y(-?\d+)z(\d+)` (matching Java regex)
/// - x coordinate: non-negative integer (0 or positive)
/// - y coordinate: signed integer (can be negative)
/// - z coordinate: non-negative integer (0 or positive)
/// 
/// # Examples
/// - `"x5y64z10"` → `[5, 64, 10]`
/// - `"x0y-64z15"` → `[0, -64, 15]`
/// - `"x-5y64z10"` → `None` (negative x is invalid)
/// - `"x5y64z-10"` → `None` (negative z is invalid)
/// 
/// # Parameters
/// - `env`: JNI environment pointer
/// - `_class`: Java class reference (unused)
/// - `key`: Java String containing the block key to parse
/// 
/// # Returns
/// - Success: A Java int array containing `[x, y, z]` coordinates
/// - Failure: `null` if the string doesn't match the expected format or parsing fails
/// 
/// # Safety
/// This function is safe to call from Java. All error conditions (invalid format,
/// invalid integers, null strings) are handled gracefully by returning null.
#[no_mangle]
pub extern "system" fn Java_ovh_paulem_krimson_utils_NativeUtil_parseBlockKey(
    mut env: JNIEnv,
    _class: JClass,
    key: JString,
) -> jintArray {
    // Convert JString to Rust String
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    // Parse the key using fast string slicing
    match parse_block_key(&key_str) {
        Some([x, y, z]) => {
            // Create a new int array and populate it
            match env.new_int_array(3) {
                Ok(arr) => {
                    let coords = [x, y, z];
                    if env.set_int_array_region(&arr, 0, &coords).is_ok() {
                        arr.into_raw()
                    } else {
                        std::ptr::null_mut()
                    }
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        None => std::ptr::null_mut(),
    }
}

/// Fast parser for block key format: x{num}y{num}z{num}
/// Uses simple string slicing without regex overhead
fn parse_block_key(key: &str) -> Option<[i32; 3]> {
    let bytes = key.as_bytes();
    
    // Must start with 'x'
    if bytes.is_empty() || bytes[0] != b'x' {
        return None;
    }

    // Find positions of 'y' and 'z'
    let mut y_pos = None;
    let mut z_pos = None;
    
    for (i, &byte) in bytes.iter().enumerate().skip(1) {
        if byte == b'y' && y_pos.is_none() {
            y_pos = Some(i);
        } else if byte == b'z' && y_pos.is_some() {
            z_pos = Some(i);
            break;
        }
    }

    let y_pos = y_pos?;
    let z_pos = z_pos?;

    // Parse x coordinate (between position 1 and y_pos) - must be non-negative
    let x_str = &key[1..y_pos];
    if x_str.is_empty() || x_str.starts_with('-') {
        return None;
    }
    let x = x_str.parse::<i32>().ok()?;

    // Parse y coordinate (between y_pos+1 and z_pos) - can be negative
    let y_str = &key[y_pos + 1..z_pos];
    if y_str.is_empty() {
        return None;
    }
    let y = y_str.parse::<i32>().ok()?;

    // Parse z coordinate (from z_pos+1 to end) - must be non-negative
    let z_str = &key[z_pos + 1..];
    if z_str.is_empty() || z_str.starts_with('-') {
        return None;
    }
    let z = z_str.parse::<i32>().ok()?;

    Some([x, y, z])
}

/// Compresses a byte array using ZLIB/Deflate compression.
/// 
/// This is a JNI function exposed to Java as `NativeUtil.compress(byte[])`.
/// It provides high-performance compression using the Rust flate2 library.
/// 
/// # Parameters
/// - `env`: JNI environment pointer
/// - `_class`: Java class reference (unused)
/// - `data`: Java byte array to compress
/// 
/// # Returns
/// - Success: A compressed Java byte array
/// - Failure: `null` if compression fails or input is invalid
/// 
/// # Safety
/// This function is safe to call from Java. All error conditions are handled
/// gracefully by returning null.
#[no_mangle]
pub extern "system" fn Java_ovh_paulem_krimson_utils_NativeUtil_compress(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jbyteArray {
    // Convert Java byte array to Rust Vec<u8>
    let input_bytes = match env.convert_byte_array(&data) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Compress the data using Deflate with balanced compression
    // Level 6 provides good compression ratio while maintaining reasonable speed
    let mut encoder = DeflateEncoder::new(Vec::new(), Compression::new(6));
    if encoder.write_all(&input_bytes).is_err() {
        return std::ptr::null_mut();
    }
    
    let compressed = match encoder.finish() {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };

    // Convert back to Java byte array
    match env.byte_array_from_slice(&compressed) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Decompresses a ZLIB/Deflate compressed byte array.
/// 
/// This is a JNI function exposed to Java as `NativeUtil.decompress(byte[])`.
/// It provides high-performance decompression using the Rust flate2 library.
/// 
/// # Parameters
/// - `env`: JNI environment pointer
/// - `_class`: Java class reference (unused)
/// - `data`: Java byte array containing compressed data
/// 
/// # Returns
/// - Success: A decompressed Java byte array
/// - Failure: `null` if decompression fails or input is invalid
/// 
/// # Safety
/// This function is safe to call from Java. All error conditions are handled
/// gracefully by returning null.
#[no_mangle]
pub extern "system" fn Java_ovh_paulem_krimson_utils_NativeUtil_decompress(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jbyteArray {
    // Convert Java byte array to Rust Vec<u8>
    let compressed_bytes = match env.convert_byte_array(&data) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Decompress the data using Inflate
    let mut decoder = DeflateDecoder::new(Vec::new());
    if decoder.write_all(&compressed_bytes).is_err() {
        return std::ptr::null_mut();
    }
    
    let decompressed = match decoder.finish() {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };

    // Convert back to Java byte array
    match env.byte_array_from_slice(&decompressed) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_valid_keys() {
        assert_eq!(parse_block_key("x5y64z10"), Some([5, 64, 10]));
        assert_eq!(parse_block_key("x0y0z0"), Some([0, 0, 0]));
        assert_eq!(parse_block_key("x15y-64z7"), Some([15, -64, 7]));
        assert_eq!(parse_block_key("x10y100z10"), Some([10, 100, 10]));
        assert_eq!(parse_block_key("x0y-1z0"), Some([0, -1, 0]));
    }

    #[test]
    fn test_parse_invalid_keys() {
        assert_eq!(parse_block_key(""), None);
        assert_eq!(parse_block_key("invalid"), None);
        assert_eq!(parse_block_key("x5y64"), None);
        assert_eq!(parse_block_key("5y64z10"), None);
        assert_eq!(parse_block_key("xy64z10"), None);
        assert_eq!(parse_block_key("x5yz10"), None);
        assert_eq!(parse_block_key("x5y64z"), None);
        assert_eq!(parse_block_key("xay64z10"), None);
        // Negative x and z should be rejected
        assert_eq!(parse_block_key("x-5y64z10"), None);
        assert_eq!(parse_block_key("x5y64z-10"), None);
        assert_eq!(parse_block_key("x-5y64z-10"), None);
    }

    #[test]
    fn test_compression_decompression() {
        let original_data = b"This is test data for compression. It should compress and decompress correctly.";
        
        // Compress
        let mut encoder = DeflateEncoder::new(Vec::new(), Compression::new(6));
        encoder.write_all(original_data).unwrap();
        let compressed = encoder.finish().unwrap();
        
        // Verify compression actually reduced size
        assert!(compressed.len() < original_data.len());
        
        // Decompress
        let mut decoder = DeflateDecoder::new(Vec::new());
        decoder.write_all(&compressed).unwrap();
        let decompressed = decoder.finish().unwrap();
        
        // Verify round-trip
        assert_eq!(original_data, &decompressed[..]);
    }

    #[test]
    fn test_compression_empty_data() {
        let empty_data = b"";
        
        let mut encoder = DeflateEncoder::new(Vec::new(), Compression::new(6));
        encoder.write_all(empty_data).unwrap();
        let compressed = encoder.finish().unwrap();
        
        // Empty data should still compress to something
        assert!(!compressed.is_empty());
    }
}

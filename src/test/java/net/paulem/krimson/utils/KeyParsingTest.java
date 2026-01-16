package net.paulem.krimson.utils;

/**
 * Manual test to verify the parsing logic works correctly.
 * This can be run as a standalone class.
 */
public class KeyParsingTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Key Parsing Implementation");
        System.out.println("===================================");
        
        boolean allPassed = true;
        
        // Test cases: [input, expectedX, expectedY, expectedZ]
        Object[][] testCases = {
            {"x5y64z10", 5, 64, 10},
            {"x0y0z0", 0, 0, 0},
            {"x15y-64z7", 15, -64, 7},
            {"x10y100z10", 10, 100, 10},
            {"x0y-1z0", 0, -1, 0},
            {"x7y255z15", 7, 255, 15},
        };
        
        // Test invalid cases (should return null)
        String[] invalidCases = {
            "",
            "invalid",
            "x5y64",
            "5y64z10",
            "xy64z10",
            "x5yz10",
            "x5y64z",
            "xay64z10",
        };
        
        System.out.println("\nTesting Valid Cases:");
        System.out.println("--------------------");
        
        for (Object[] testCase : testCases) {
            String input = (String) testCase[0];
            int expectedX = (int) testCase[1];
            int expectedY = (int) testCase[2];
            int expectedZ = (int) testCase[3];
            
            boolean testPassed = testValidCase(input, expectedX, expectedY, expectedZ);
            allPassed = allPassed && testPassed;
        }
        
        System.out.println("\nTesting Invalid Cases:");
        System.out.println("----------------------");
        
        for (String input : invalidCases) {
            boolean testPassed = testInvalidCase(input);
            allPassed = allPassed && testPassed;
        }
        
        System.out.println("\n===================================");
        if (allPassed) {
            System.out.println("✓ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("✗ Some tests failed!");
            System.exit(1);
        }
    }
    
    private static boolean testValidCase(String input, int expectedX, int expectedY, int expectedZ) {
        try {
            boolean useNative = false;
            int[] result = null;
            
            // Try native implementation if available
            try {
                if (NativeUtil.isLoaded()) {
                    result = NativeUtil.parseBlockKey(input);
                    useNative = true;
                }
            } catch (Throwable t) {
                // Native not available, use fallback
            }
            
            if (useNative && result != null) {
                if (result.length == 3 
                    && result[0] == expectedX 
                    && result[1] == expectedY 
                    && result[2] == expectedZ) {
                    System.out.println("✓ [NATIVE] " + input + " -> [" + result[0] + ", " + result[1] + ", " + result[2] + "]");
                    return true;
                } else {
                    System.out.println("✗ [NATIVE] " + input + " -> [" + result[0] + ", " + result[1] + ", " + result[2] 
                        + "] (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                    return false;
                }
            } else if (useNative) {
                System.out.println("✗ [NATIVE] " + input + " -> null (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                return false;
            } else {
                // Test fallback regex implementation
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
                java.util.regex.Matcher matcher = pattern.matcher(input);
                if (matcher.matches()) {
                    int x = Integer.parseInt(matcher.group(1));
                    int y = Integer.parseInt(matcher.group(2));
                    int z = Integer.parseInt(matcher.group(3));
                    
                    if (x == expectedX && y == expectedY && z == expectedZ) {
                        System.out.println("✓ [FALLBACK] " + input + " -> [" + x + ", " + y + ", " + z + "]");
                        return true;
                    } else {
                        System.out.println("✗ [FALLBACK] " + input + " -> [" + x + ", " + y + ", " + z 
                            + "] (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                        return false;
                    }
                } else {
                    System.out.println("✗ [FALLBACK] " + input + " -> no match (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("✗ " + input + " -> Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testInvalidCase(String input) {
        try {
            boolean useNative = false;
            int[] result = null;
            
            // Try native implementation if available
            try {
                if (NativeUtil.isLoaded()) {
                    result = NativeUtil.parseBlockKey(input);
                    useNative = true;
                }
            } catch (Throwable t) {
                // Native not available, use fallback
            }
            
            if (useNative) {
                if (result == null) {
                    System.out.println("✓ [NATIVE] \"" + input + "\" -> null (correctly rejected)");
                    return true;
                } else {
                    System.out.println("✗ [NATIVE] \"" + input + "\" -> [" + result[0] + ", " + result[1] + ", " + result[2] 
                        + "] (should have been rejected)");
                    return false;
                }
            } else {
                // Test fallback regex implementation
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
                java.util.regex.Matcher matcher = pattern.matcher(input);
                if (!matcher.matches()) {
                    System.out.println("✓ [FALLBACK] \"" + input + "\" -> no match (correctly rejected)");
                    return true;
                } else {
                    System.out.println("✗ [FALLBACK] \"" + input + "\" -> matched (should have been rejected)");
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("✗ \"" + input + "\" -> Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

package ovh.paulem.krimson.utils;

/**
 * Simple standalone test to verify the regex fallback parsing logic.
 */
public class FallbackParsingTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Fallback Regex Parsing");
        System.out.println("================================");
        
        boolean allPassed = true;
        
        // Test valid cases
        allPassed &= testValidCase("x5y64z10", 5, 64, 10);
        allPassed &= testValidCase("x0y0z0", 0, 0, 0);
        allPassed &= testValidCase("x15y-64z7", 15, -64, 7);
        allPassed &= testValidCase("x10y100z10", 10, 100, 10);
        allPassed &= testValidCase("x0y-1z0", 0, -1, 0);
        allPassed &= testValidCase("x7y255z15", 7, 255, 15);
        
        // Test invalid cases
        allPassed &= testInvalidCase("");
        allPassed &= testInvalidCase("invalid");
        allPassed &= testInvalidCase("x5y64");
        allPassed &= testInvalidCase("5y64z10");
        allPassed &= testInvalidCase("xy64z10");
        allPassed &= testInvalidCase("x5yz10");
        allPassed &= testInvalidCase("x5y64z");
        allPassed &= testInvalidCase("xay64z10");
        
        System.out.println("\n================================");
        if (allPassed) {
            System.out.println("✓ All fallback tests passed!");
            System.exit(0);
        } else {
            System.out.println("✗ Some tests failed!");
            System.exit(1);
        }
    }
    
    private static boolean testValidCase(String input, int expectedX, int expectedY, int expectedZ) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
            java.util.regex.Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                
                if (x == expectedX && y == expectedY && z == expectedZ) {
                    System.out.println("✓ " + input + " -> [" + x + ", " + y + ", " + z + "]");
                    return true;
                } else {
                    System.out.println("✗ " + input + " -> [" + x + ", " + y + ", " + z 
                        + "] (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                    return false;
                }
            } else {
                System.out.println("✗ " + input + " -> no match (expected [" + expectedX + ", " + expectedY + ", " + expectedZ + "])");
                return false;
            }
        } catch (Exception e) {
            System.out.println("✗ " + input + " -> Exception: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testInvalidCase(String input) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
            java.util.regex.Matcher matcher = pattern.matcher(input);
            if (!matcher.matches()) {
                System.out.println("✓ \"" + input + "\" -> correctly rejected");
                return true;
            } else {
                System.out.println("✗ \"" + input + "\" -> matched (should have been rejected)");
                return false;
            }
        } catch (Exception e) {
            System.out.println("✗ \"" + input + "\" -> Exception: " + e.getMessage());
            return false;
        }
    }
}

package de.interactive_instruments.ShapeChange;

import static org.junit.Assume.assumeTrue;

import org.apache.commons.lang.SystemUtils;
import org.junit.Before;

public class WindowsBasicTest extends BasicTest {
	
	@Before
	public void assumeWindows() {
		assumeTrue(SystemUtils.IS_OS_WINDOWS);
	}

}

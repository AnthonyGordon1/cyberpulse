package com.anthonygordon.cyberpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "cyberpulse.sync.enabled=false")
class CyberpulseApplicationTests {

	@Test
	void contextLoads() {
	}

}

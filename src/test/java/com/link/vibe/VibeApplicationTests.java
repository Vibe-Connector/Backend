package com.link.vibe;

import com.link.vibe.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRedisConfig.class)
class VibeApplicationTests {

	@Test
	void contextLoads() {
	}

}

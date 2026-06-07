package attune;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AttuneApplicationTests {

	@Test
	void contextLoads() {
		assertEquals("Asia/Seoul", TimeZone.getDefault().getID());
	}

}

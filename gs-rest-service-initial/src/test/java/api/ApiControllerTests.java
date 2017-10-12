package api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerTests {
	
    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));
    
    private HttpMessageConverter mappingJackson2HttpMessageConverter;
	
	@Autowired
    private MockMvc mockMvc;
	
	@Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
            .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
            .findAny()
            .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }
	
	@Test
	public void postFirstTransactionShouldReturnHttpStatusCode201() throws Exception {
		Transaction t = new Transaction(System.currentTimeMillis(), 10);
		
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t)))
		.andExpect(status().is(201));
	}
	
	@Test
	public void postSecondTransactionsShouldReturnHttpStatusCode201() throws Exception {
		Transaction t2 = new Transaction(System.currentTimeMillis(), 20);
		Transaction t3 = new Transaction(System.currentTimeMillis() + 1 * 1000, 30);
		
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t2)));
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t3)))
		.andExpect(status().is(201));
	}
	
	@Test
	public void postTwoTransactionsShouldReturnHttpStatusCode204() throws Exception {
		Transaction t4 = new Transaction(System.currentTimeMillis(), 40);
		Transaction t5 = new Transaction(System.currentTimeMillis() + ControllerManager.INTERVAL_SECS * 1000 + 100, 50);
		
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t4)));
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t5)))
		.andExpect(status().is(204));
	}
	
	@Test
	public void getStatisticsShouldReturnStatisticsMessage() throws Exception {
		Transaction t1 = new Transaction(System.currentTimeMillis(), 10);
		Transaction t2 = new Transaction(System.currentTimeMillis() + 2 * 1000, 20);
		Transaction t3 = new Transaction(System.currentTimeMillis() + 3 * 1000, 30);
		
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t1)));
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t2)));
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t3)));
		
		this.mockMvc.perform
		(
			get("/api/statistics/")).andDo(print())
		    .andExpect(status().isOk())
			.andExpect(jsonPath("$.sum", is(60.0)))
			.andExpect(jsonPath("$.avg", is(20.0)))
			.andExpect(jsonPath("$.max", is(30.0)))
			.andExpect(jsonPath("$.min", is(10.0)))
			.andExpect(jsonPath("$.count", is(3))
		);		
					
	}
	
	@Test
	public void getStatisticsCacheNoDataShouldReturnNotFound() throws Exception {
		this.mockMvc.perform(get("/api/statistics/")).andExpect(status().isNotFound());
	}
	
	@Test
	public void getStatisticsCacheDataExpiredShouldReturnNotFound() throws Exception {		
		Transaction t = new Transaction(System.currentTimeMillis() - (ControllerManager.INTERVAL_SECS * 1000 + 100), 50);
		this.mockMvc.perform(post("/api/transactions").contentType(contentType).content(json(t)));
		this.mockMvc.perform(get("/api/statistics/")).andExpect(status().isNotFound());
	}
	
	@Test
	public void getStatisticsAfterMultithreadPostShouldReturnStatisticsMessage() throws Exception {
		// simulate multiple user perform post transactions and get statistics request concurrently
		// assume that totals post requests more often than get requests
		ExecutorService pool = Executors.newCachedThreadPool();
		 List<Runnable> taskList = new ArrayList<Runnable>();
		
		for(int i = 1; i <= 50; i++) {
										
			if(i % 5 == 0) {
				// post request
				Transaction t = new Transaction(System.currentTimeMillis() + i * 10, i);				
				taskList.add(new Handler("/api/transactions", json(t), this.contentType, this.mockMvc));				
			}
			
			if(i % 10 == 0) {
				taskList.add(new Handler("/api/statistics/", null, this.contentType, this.mockMvc));
			}
						
			
		}// end for
		
		for (int i = taskList.size() - 1; i >= 0; i--) {
		        pool.execute(taskList.get(i));
		}
		
		pool.shutdown();
		boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);
		if(finished) {
			this.mockMvc.perform
			(
				get("/api/statistics/")).andDo(print())
			    .andExpect(status().isOk())
				.andExpect(jsonPath("$.sum", is(275.0)))
				.andExpect(jsonPath("$.avg", is(27.5)))
				.andExpect(jsonPath("$.max", is(50.0)))
				.andExpect(jsonPath("$.min", is(5.0)))
				.andExpect(jsonPath("$.count", is(10))
			);		
		}
	}
	
	protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}

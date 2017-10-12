package api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class Handler implements Runnable {

	private String uri;
	private String jsonString;
	private MediaType contentType;
	private MockMvc mockMvc;
	
	public Handler(String uri, String json, MediaType contentType, MockMvc mockMvc) {
		this.uri = uri;
		this.jsonString = json;
		this.contentType = contentType;
		this.mockMvc = mockMvc;
	}
	
	//TODO use Enum to handle the post/get actions
	@Override
	public void run() {
		if(uri.contains("transactions")) {
			try {
				mockMvc.perform(post(uri).contentType(contentType).content(jsonString));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(uri.contains("statistics")) {
			try {
				mockMvc.perform(get(uri)).andDo(print());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

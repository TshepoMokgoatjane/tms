package za.co.tms.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configure")
public class ConfigurationController {

	@Autowired
	private Environment environment;
	
	@Value("${spring.datasource.url}")
	private String url;
	
	@Value("${spring.datasource.username}")
	private String username;
	
	@Value("${spring.datasource.password}")
	private String password;
	
	
	@GetMapping("/{type}")
	public String propertiesValues(@PathVariable("type") Integer type) {
		if (type == 1) {
			return "Application Properties Values: \nURL: " 
			+ environment.getProperty("spring.datasource.url") 
			+ "\nUsername: " + environment.getProperty("spring.datasource.username")
			+ "\nPassword: " + environment.getProperty("spring.datasource.password");
		} else {
			return "Unknown Property Values!";
		}
	}
}

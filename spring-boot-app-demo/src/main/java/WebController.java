import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    private final AService aService;

    public WebController(AService aService) {
        this.aService = aService;
    }

    @GetMapping("/")
    String url() {
        return aService.getString();
    }


}

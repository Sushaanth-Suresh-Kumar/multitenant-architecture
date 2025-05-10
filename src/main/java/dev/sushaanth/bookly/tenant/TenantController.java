package dev.sushaanth.bookly.tenant;
import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
public class TenantController {

//    private UserRepository userRepository;
//
//    public TenantController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @GetMapping
//    public List<User> getUsers() {
//        return userRepository.findAll();
//    }
//
//    @PostMapping
//    public User createUser(@RequestBody User user) {
//        return userRepository.save(user);
//    }

    @GetMapping
    public String getTenant() {
        return TenantContext.getTenantId();
    }
}
package com.gifttogether.wishlist;

import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wishlist.domain.WishlistItem;
import com.gifttogether.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wishlist-api-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class WishlistApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    WishlistItemRepository wishlistItemRepository;

    @Test
    void 자신의_위시리스트를_조회한다() throws Exception {

        User user = userRepository.save(
                new User(
                        "piwoori",
                        "piwoori@test.com"
                )
        );

        Product product = productRepository.save(
                new Product(
                        "AirPods Pro",
                        150_000L,
                        "https://example.com/airpods.jpg"
                )
        );

        wishlistItemRepository.save(
                new WishlistItem(
                        user,
                        product
                )
        );

        mockMvc.perform(
                        get("/api/wishlist")
                                .header(
                                        "X-USER-ID",
                                        user.getId()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId")
                        .value(product.getId()))
                .andExpect(jsonPath("$[0].productName")
                        .value("AirPods Pro"))
                .andExpect(jsonPath("$[0].price")
                        .value(150000))
                .andExpect(jsonPath("$[0].wishlistItemId")
                        .value(org.hamcrest.Matchers.notNullValue()));
    }
}
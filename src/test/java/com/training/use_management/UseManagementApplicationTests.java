package com.training.use_management;

import com.training.use_management.entity.User;
import com.training.use_management.service.UserService;
import com.training.use_management.utils.PasswordUtil;
import com.training.use_management.utils.ValidationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.BDDAssumptions.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class UseManagementApplicationTests {

    @Test
    void contextLoads() {
    }

//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private ValidationUtil validationUtil;
//
//    @Autowired
//    private PasswordUtil passwordUtil;
//    @Test
//    public void testUserValidation() {
//        // Given
//        User user = new User("john", "password");
//        // When
//        boolean result = userService.validateUser(user);
//
//        // Then
//        assertTrue(result);
//    }
//    @Test
//    public void testLoginEndpoint() {
//        given()
//                .contentType("application/json")
//                .body("{\"username\": \"john\", \"password\": \"password\"}")
//                .when()
//                .post("/api/auth/login")
//                .then()
//                .statusCode(200)
//                .body("token", notNullValue());
//    }
//
//    @Test
//    public void testValidateEmail() {
//        String validEmail = "test@example.com";
//        assertTrue(ValidationUtil.isEmailValid(validEmail));
//    }
//
//    @Test
//    public void testPasswordEncryption() {
//        String rawPassword = "mypassword";
//        String encryptedPassword = PasswordUtil.encodePassword(rawPassword);
//        assertNotEquals(rawPassword, encryptedPassword);
//    }
//
//    @Test
//    public void testCreateUser() {
//        User user = userService.createUser("testUser", "password", "ROLE_USER");
//        assertNotNull(user.getId());
//        assertEquals("testUser", user.getUsername());
//    }
//
//    @Test(expected = UserNotFoundException.class)
//    public void testFindNonExistingUser() {
//        userService.findUserById(-1L);
//    }
//
//    @Test
//    public void testSaveUser() {
//        User user = new User("testUser", "password");
//        when(userRepository.save(user)).thenReturn(user);
//        assertNotNull(userService.saveUser(user));
//    }
//
//    @Test
//    public void testUserRoleAssignment() {
//        User user = userService.assignRoleToUser("testUser", "ROLE_ADMIN");
//        assertTrue(user.getRoles().contains("ROLE_ADMIN"));
//    }
//
//    @Test(timeout = 1000) // Đảm bảo phương thức thực thi trong 1 giây.
//    public void testPerformanceOfUserCreation() {
//        userService.createUser("fastUser", "password", "ROLE_USER");
//    }


}

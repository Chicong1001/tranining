package com.training.use_management.dto.responseDTO;

import com.training.use_management.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class UserProfileDTO {

    private Long id;
    private String username;
    private String email;
    private List<Role> roles; // Keep this as List<Role>
    private String message;

    public UserProfileDTO(Long id, String username, String email, Set<Role> roles, String message) {
        this.id = id;
        this.username = username;
        this.email = email;
        // Keep the roles as a List<Role> (no need for conversion)
        this.roles = new ArrayList<>(roles); // Convert Set<Role> to List<Role>
        this.message = message;
    }
}

package com.github.admarple.barbeque.lambda;

import com.amazonaws.services.identitymanagement.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Wither
@NoArgsConstructor
@AllArgsConstructor
public class ListRolesResponse {
    private List<Role> roles;
}

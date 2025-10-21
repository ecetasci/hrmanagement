package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.Role;

public record LoginResponseDto  (String token,String name,String email, Role role )  {

}

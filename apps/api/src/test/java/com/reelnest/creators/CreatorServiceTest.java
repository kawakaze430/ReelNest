package com.reelnest.creators;

import com.reelnest.users.CurrentUserService;
import com.reelnest.users.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatorServiceTest {
    private final CreatorRepository repository=mock(CreatorRepository.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final CreatorService service=new CreatorService(repository,current,mock(JdbcTemplate.class));
    private final Jwt jwt=Jwt.withTokenValue("example").header("alg","none").subject("creator-user").build();

    @Test void rejectsAccountsWithoutApprovedCreatorApplication() {
        UserEntity user=new UserEntity("creator-user","creator@example.invalid");
        when(current.resolve(jwt)).thenReturn(user);
        when(repository.findByUserId(user.getId())).thenReturn(Optional.empty());
        var absent=assertThrows(ResponseStatusException.class,()->service.requireApproved(jwt));
        assertEquals(HttpStatus.FORBIDDEN,absent.getStatusCode());
        CreatorEntity pending=new CreatorEntity(user.getId(),"Studio","Creator Person","creator@example.invalid",null,
            "I have written distribution agreements for my films.");
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(pending));
        var waiting=assertThrows(ResponseStatusException.class,()->service.requireApproved(jwt));
        assertEquals(HttpStatus.FORBIDDEN,waiting.getStatusCode());
        pending.review("APPROVED","Verified by admin");
        assertEquals(pending.getId(),service.requireApproved(jwt).getId());
    }
}

package com.reelnest.profiles;

import com.reelnest.users.CurrentUserService;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Database-free regression for the profile-deletion PIN contract.
 * Real PostgreSQL transactions/lockout persistence still require an integration test.
 */
class ProfilePinControllerTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final CurrentUserService users=mock(CurrentUserService.class);
    private final ProfilePinController controller=new ProfilePinController(jdbc,users);
    private final UUID user=UUID.randomUUID(),profile=UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private void ownerRow(byte[] salt,byte[] hash,int failures,Instant lockedUntil) throws Exception {
        ResultSet rs=mock(ResultSet.class);
        when(rs.getBoolean(1)).thenReturn(false);
        when(rs.getBytes(2)).thenReturn(salt);
        when(rs.getBytes(3)).thenReturn(hash);
        when(rs.getInt(4)).thenReturn(failures);
        if(lockedUntil!=null)when(rs.getTimestamp(5)).thenReturn(Timestamp.from(lockedUntil));
        when(jdbc.query(contains("FOR UPDATE"),any(RowMapper.class),eq(profile),eq(user)))
            .thenAnswer(inv -> {
                RowMapper<?> mapper=inv.getArgument(1);
                return List.of(mapper.mapRow(rs,0));
            });
    }

    @Test
    void ownerCanRemoveProfileWithoutConfiguredPin() throws Exception {
        ownerRow(null,null,0,null);
        assertDoesNotThrow(() -> controller.requireForSensitiveAction(user,profile,null));
        verify(jdbc,never()).update(anyString(),any(),any());
    }

    @Test
    void correctPinVerifiesBeforeSensitiveAction() throws Exception {
        byte[] salt=ProfilePinPolicy.salt();
        ownerRow(salt,ProfilePinPolicy.derive("287594",salt),0,null);
        assertDoesNotThrow(() -> controller.requireForSensitiveAction(user,profile,"287594"));
        verify(jdbc).update(contains("pin_failed_attempts=0"),eq(profile),eq(user));
    }

    @Test
    void invalidPinIsDeniedAndIncrementsLockoutCounter() throws Exception {
        byte[] salt=ProfilePinPolicy.salt();
        ownerRow(salt,ProfilePinPolicy.derive("287594",salt),4,null);
        ResponseStatusException denied=assertThrows(ResponseStatusException.class,
            () -> controller.requireForSensitiveAction(user,profile,"000000"));
        assertEquals(HttpStatus.FORBIDDEN,denied.getStatusCode());
        verify(jdbc).update(contains("pin_failed_attempts=?"),eq(5),any(Timestamp.class),eq(profile),eq(user));
    }

    @Test
    void lockoutDeniesEvenTheCorrectPin() throws Exception {
        byte[] salt=ProfilePinPolicy.salt();
        ownerRow(salt,ProfilePinPolicy.derive("287594",salt),5,Instant.now().plusSeconds(300));
        ResponseStatusException denied=assertThrows(ResponseStatusException.class,
            () -> controller.requireForSensitiveAction(user,profile,"287594"));
        assertEquals(HttpStatus.FORBIDDEN,denied.getStatusCode());
        verify(jdbc,never()).update(anyString(),any(),any());
    }

    @Test
    void profileNotOwnedByUserIsNotFound() throws Exception {
        when(jdbc.query(contains("FOR UPDATE"),any(RowMapper.class),eq(profile),eq(user)))
            .thenReturn(List.of());
        ResponseStatusException denied=assertThrows(ResponseStatusException.class,
            () -> controller.requireForSensitiveAction(user,profile,"287594"));
        assertEquals(HttpStatus.NOT_FOUND,denied.getStatusCode());
    }
}

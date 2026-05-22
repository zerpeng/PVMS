package com.zzp.pvms.auth.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.SysUser;
import com.zzp.pvms.auth.mapper.SysUserMapper;
import com.zzp.pvms.auth.service.SysRoleService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:pvms-secret-key-for-jwt-authentication-2026-zzp}")
    private String secret;

    @Value("${jwt.expiration:604800}")
    private Long expiration;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleService sysRoleService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, userDetails.getUsername())
                .last("LIMIT 1"));
        String roleKey = user == null ? resolveRoleKey(userDetails) : user.getRoleKey();
        SysRole role = sysRoleService.getActiveRole(roleKey);
        claims.put("nickname", user == null ? "" : user.getNickname());
        claims.put("roleKey", roleKey);
        claims.put("roleName", role == null ? roleKey : role.getRoleName());
        claims.put("superAdmin", role != null && Integer.valueOf(1).equals(role.getSuperAdmin()));
        claims.put("visitor", role != null && Integer.valueOf(1).equals(role.getVisitor()));
        claims.put("menuKeys", role == null ? "" : role.getMenuKeys());
        claims.put("directoryIds", role == null ? "" : role.getDirectoryIds());
        claims.put("albumIds", role == null ? "" : role.getAlbumIds());
        claims.put("tagIds", role == null ? "" : role.getTagIds());
        return createToken(claims, userDetails.getUsername());
    }

    private String resolveRoleKey(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("USER");
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}

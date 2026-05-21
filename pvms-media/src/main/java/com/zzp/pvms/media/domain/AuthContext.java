package com.zzp.pvms.media.domain;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Data
public class AuthContext {
    private String username;
    private String roleKey;
    private boolean superAdmin;
    private boolean visitor;
    private List<Long> directoryIds;
    private List<Long> albumIds;
    private List<Long> tagIds;

    public static AuthContext of(String username, String roleKey, String superAdmin, String visitor,
                                 String directoryIds, String albumIds, String tagIds) {
        AuthContext context = new AuthContext();
        context.setUsername(StringUtils.hasText(username) ? username : "anonymous");
        context.setRoleKey(StringUtils.hasText(roleKey) ? roleKey : "USER");
        context.setSuperAdmin(Boolean.parseBoolean(superAdmin) || "ADMIN".equals(context.getRoleKey()));
        context.setVisitor(Boolean.parseBoolean(visitor) || "VISITOR".equals(context.getRoleKey()));
        context.setDirectoryIds(parseIds(directoryIds));
        context.setAlbumIds(parseIds(albumIds));
        context.setTagIds(parseIds(tagIds));
        return context;
    }

    public boolean canWrite() {
        return !visitor;
    }

    private static List<Long> parseIds(String value) {
        if (!StringUtils.hasText(value) || "null".equalsIgnoreCase(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }
}

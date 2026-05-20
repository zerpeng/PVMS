package com.zzp.pvms.auth.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserImportResult {
    private int created;
    private int updated;
    private int skipped;
    private List<String> errors = new ArrayList<>();

    public void addCreated() {
        created++;
    }

    public void addUpdated() {
        updated++;
    }

    public void addSkipped() {
        skipped++;
    }

    public void addError(String error) {
        errors.add(error);
    }
}

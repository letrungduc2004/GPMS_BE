package com.example.tlu_gpms_system.features.faculty.service.scope;

import com.example.tlu_gpms_system.features.faculty.entity.Major;

import java.util.Set;

/**
 * Phạm vi ngành mà người dùng hiện tại được thao tác dữ liệu sinh viên, giáo viên.
 * Value object bất biến, chỉ dùng nội bộ (không qua Jackson):
 * - all = true: có quyền ACCESS_ALL_MAJORS (Admin), thao tác mọi ngành
 * - all = false: chỉ thao tác các ngành trong majorIds; majorIds rỗng thì mọi thao tác ghi đều bị chặn
 */
public record MajorScope(boolean all, Set<Long> majorIds) {

    public MajorScope {
        majorIds = majorIds == null ? Set.of() : Set.copyOf(majorIds);
    }

    public static MajorScope allMajors() {
        return new MajorScope(true, Set.of());
    }
    public static MajorScope of(Set<Long> majorIds) {
        return new MajorScope(false, majorIds);
    }
    public boolean allows(Major major) {
        return all || (major != null && allowsMajorId(major.getId()));
    }
    public boolean allowsMajorId(Long majorId) {
        return all || (majorId != null && majorIds.contains(majorId));
    }

    // Không phải Admin và không phụ trách ngành nào
    public boolean isEmpty() {
        return !all && majorIds.isEmpty();
    }
}

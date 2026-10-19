package com.example.tlu_gpms_system.common.utils.excel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class DuplicateUtils {

    private DuplicateUtils() {
    }

    // Trả về vị trí của TẤT CẢ các phần tử có key bị lặp (không giữ lại phần tử đầu tiên).
    // Key null được bỏ qua (VD: ô đã lỗi ở bước trước thì không tính trùng)
    public static <K> Set<Integer> findDuplicateIndexes(List<K> keys) {
        Map<K, Integer> counts = new HashMap<>();
        for (K key : keys) {
            if (key != null) {
                counts.merge(key, 1, Integer::sum);
            }
        }
        Set<Integer> indexes = new TreeSet<>();
        for (int i = 0; i < keys.size(); i++) {
            K key = keys.get(i);
            if (key != null && counts.get(key) > 1) {
                indexes.add(i);
            }
        }
        return indexes;
    }
}

package org.example.cardealershipjavafx.util;

public class FkSelectionResult {
    private static Object selectedId;

    public static Object getSelectedId() {
        Object id = selectedId;
        selectedId = null; // Сбросить после получения
        return id;
    }

    public static void setSelectedId(Object selectedId) {
        FkSelectionResult.selectedId = selectedId;
    }
}

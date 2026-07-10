package Interfaces;

public interface ICodeSource {
    String getDisplayName();      // для UI и имени листа Excel
    String readText() throws Exception; // читает содержимое заново при каждом вызове - не кэшируется
}

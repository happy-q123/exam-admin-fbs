import com.snow.flake.algorithm.Snowflake;

public class SnowTest {
    public static void main(String[] args) {
        Snowflake snowflake = new Snowflake();
        System.out.println(Long.valueOf(snowflake.nextId()).toString().length());
    }
}

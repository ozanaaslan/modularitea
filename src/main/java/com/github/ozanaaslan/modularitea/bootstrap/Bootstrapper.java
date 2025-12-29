package com.github.ozanaaslan.modularitea.bootstrap;
import com.github.ozanaaslan.modularitea.AbstractModulariteaApplication;
import java.io.InputStream;
import java.util.Properties;
public class Bootstrapper {

    public static void main(String[] args) {
        Properties props = new Properties();

        try (InputStream input = Bootstrapper.class.getClassLoader().getResourceAsStream("modularitea.properties")) {
            if (input == null) {
                System.err.println("Error: modularitea.properties not found in resources root!");
                return;
            }

            props.load(input);
            String mainClassName = props.getProperty("mainClass");

            if (mainClassName == null || mainClassName.isEmpty()) {
                System.err.println("Error: mainClass property is missing in modularitea.properties!");
                return;
            }

            Class<?> clazz = Class.forName(mainClassName);

            if (AbstractModulariteaApplication.class.isAssignableFrom(clazz)) {
                AbstractModulariteaApplication app = (AbstractModulariteaApplication) clazz.getDeclaredConstructor().newInstance();
                app.initialize();
            } else {
                System.err.println("Error: " + mainClassName + " does not extend AbstractModulariteaApplication!");
            }

        } catch (Exception e) {
            System.err.println("Critical failure during bootstrap sequence:");
            e.printStackTrace();
        }
    }
}

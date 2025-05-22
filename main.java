import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

// --- Ingredient Class ---
class Ingredient {
    private String nombre;
    private double cantidad;
    private String unidad; // Keep unit for internal calculations

    public Ingredient(String nombre, double cantidad, String unidad) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public String getNombre() {
        return nombre;
    }

    public double getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public void addCantidad(double value) {
        this.cantidad += value;
    }

    @Override
    public String toString() {
        // Simplified display for ingredients
        if (cantidad % 1 == 0) { // Check if quantity is a whole number
            return (int)cantidad + " " + nombre; // Display as integer
        }
        return String.format("%.1f", cantidad) + " " + nombre; // Display with one decimal for non-whole numbers
    }

    // Keep equals and hashCode for accurate comparison in collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return nombre.equalsIgnoreCase(that.nombre) && unidad.equalsIgnoreCase(that.unidad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.toLowerCase(), unidad.toLowerCase());
    }
}

// --- Recipe Class ---
class Recipe {
    private String nombre;
    private List<Ingredient> ingredientes;

    public Recipe(String nombre, List<Ingredient> ingredientes) {
        this.nombre = nombre;
        this.ingredientes = ingredientes;
    }

    public String getNombre() {
        return nombre;
    }

    public List<Ingredient> getIngredientes() {
        return ingredientes;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

// --- DailyMenu Class ---
class DailyMenu {
    private String dia;
    private Recipe receta;

    public DailyMenu(String dia, Recipe receta) {
        this.dia = dia;
        this.receta = receta;
    }

    public String getDia() {
        return dia;
    }

    public Recipe getReceta() {
        return receta;
    }

    @Override
    public String toString() {
        return dia + ": " + receta.getNombre();
    }
}

// --- ShoppingPlanner Class ---
class ShoppingPlanner {
    private List<DailyMenu> menuSemanal;
    private List<Ingredient> inventario;

    public ShoppingPlanner() {
        this.menuSemanal = new ArrayList<>();
        this.inventario = new ArrayList<>();
    }

    public void addDailyMenu(DailyMenu dailyMenu) {
        this.menuSemanal.add(dailyMenu);
    }

    public void addInventarioItem(Ingredient item) {
        // Check if the item already exists in inventory (same name and unit)
        Optional<Ingredient> existingItem = inventario.stream()
            .filter(i -> i.getNombre().equalsIgnoreCase(item.getNombre()) && i.getUnidad().equalsIgnoreCase(item.getUnidad()))
            .findFirst();

        if (existingItem.isPresent()) {
            // If exists, add to its quantity
            existingItem.get().addCantidad(item.getCantidad());
        } else {
            // Otherwise, add as a new item
            this.inventario.add(item);
        }
    }


    public List<DailyMenu> getMenuSemanal() {
        return menuSemanal;
    }

    public List<Ingredient> getInventario() {
        return inventario;
    }

    public List<Ingredient> calcularListaDeCompras() {
        Map<String, Map<String, Double>> necesidades = new HashMap<>(); // nombre -> unidad -> cantidad

        // 1. Recopilar todas las necesidades del menú
        for (DailyMenu dailyMenu : menuSemanal) {
            for (Ingredient ing : dailyMenu.getReceta().getIngredientes()) {
                necesidades.computeIfAbsent(ing.getNombre().toLowerCase(), k -> new HashMap<>())
                        .merge(ing.getUnidad().toLowerCase(), ing.getCantidad(), Double::sum);
            }
        }

        // 2. Restar del inventario
        for (Ingredient invIng : inventario) {
            String invNombre = invIng.getNombre().toLowerCase();
            String invUnidad = invIng.getUnidad().toLowerCase();
            double invCantidad = invIng.getCantidad();

            if (necesidades.containsKey(invNombre) && necesidades.get(invNombre).containsKey(invUnidad)) {
                double needed = necesidades.get(invNombre).get(invUnidad);
                double remaining = needed - invCantidad;
                if (remaining > 0) {
                    necesidades.get(invNombre).put(invUnidad, remaining);
                } else {
                    necesidades.get(invNombre).remove(invUnidad); // No necesitamos más de esto
                    if (necesidades.get(invNombre).isEmpty()) {
                        necesidades.remove(invNombre);
                    }
                }
            }
        }

        // 3. Convertir las necesidades restantes a una lista de ingredientes
        List<Ingredient> listaCompras = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entryNombre : necesidades.entrySet()) {
            String nombre = entryNombre.getKey();
            for (Map.Entry<String, Double> entryUnidad : entryNombre.getValue().entrySet()) {
                String unidad = entryUnidad.getKey();
                double cantidad = entryUnidad.getValue();
                listaCompras.add(new Ingredient(nombre, cantidad, unidad));
            }
        }
        return listaCompras;
    }
}

// --- Main Class (User Interface) ---
public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static ShoppingPlanner planner = new ShoppingPlanner();

    public static void main(String[] args) {
        // --- Cargar datos de ejemplo ---
        cargarDatosEjemplo();
        // --- Fin Cargar datos de ejemplo ---

        int opcion;
        do {
            menuPrincipal(); // Call the new menu display method
            opcion = obtenerOpcionUsuario();

            switch (opcion) {
                case 1:
                    verMenuSemanal();
                    break;
                case 2:
                    verInventarioActual();
                    break;
                case 3:
                    generarYMostrarListaCompras();
                    break;
                case 4:
                    agregarRecetaAMenu();
                    break;
                case 5:
                    agregarItemAInventario();
                    break;
                case 6:
                    System.out.println("¡Hasta luego!");
                    break;
                default:
                    System.out.println("Opción no válida. Inténtalo de nuevo.");
            }
            System.out.println("\n--- Presiona Enter para continuar ---");
            scanner.nextLine(); // Consumir el salto de línea pendiente
        } while (opcion != 6);

        scanner.close();
    }

    // New menu display method
    private static void menuPrincipal() {
        System.out.println("\n=== Planificador de Compras Semanales ===");
        System.out.println("1. Mostrar menú semanal");
        System.out.println("2. Mostrar inventario disponible");
        System.out.println("3. Generar lista de compras");
        System.out.println("4. Agregar nueva receta");
        System.out.println("5. Agregar items al inventario");
        System.out.println("6. Salir");
        System.out.print("\nSeleccione una opción: ");
    }

    private static int obtenerOpcionUsuario() {
        while (!scanner.hasNextInt()) {
            System.out.println("Entrada no válida. Por favor, ingresa un número.");
            scanner.next(); // Consumir la entrada inválida
        }
        int opcion = scanner.nextInt();
        scanner.nextLine(); // Consumir el salto de línea
        return opcion;
    }

    private static void verMenuSemanal() {
        if (planner.getMenuSemanal().isEmpty()) {
            System.out.println("El menú semanal está vacío.");
            return;
        }
        System.out.println("\n--- Menú Semanal ---");
        for (int i = 0; i < planner.getMenuSemanal().size(); i++) {
            DailyMenu dm = planner.getMenuSemanal().get(i);
            System.out.println((i + 1) + ". " + dm.getDia() + ": " + dm.getReceta().getNombre());
        }

        System.out.print("\nSelecciona el número del día para ver el detalle (o 0 para volver al menú principal): ");
        int opcionDia = obtenerOpcionUsuario();

        if (opcionDia > 0 && opcionDia <= planner.getMenuSemanal().size()) {
            verDetalleRecetaPorDia(opcionDia - 1); // Restar 1 para el índice de la lista
        } else if (opcionDia != 0) {
            System.out.println("Número de día no válido.");
        }
    }

    private static void verDetalleRecetaPorDia(int index) {
        DailyMenu dailyMenu = planner.getMenuSemanal().get(index);
        System.out.println("\n--- Detalle de Receta para " + dailyMenu.getDia() + " ---");
        System.out.println("Receta: " + dailyMenu.getReceta().getNombre());
        System.out.println("\nIngredientes Necesarios:");
        for (Ingredient ing : dailyMenu.getReceta().getIngredientes()) {
            System.out.println("  - " + ing.getNombre() + ": " + (ing.getCantidad() % 1 == 0 ? (int)ing.getCantidad() : String.format("%.1f", ing.getCantidad())));
        }

        System.out.println("\nComparación con tu Inventario:");
        for (Ingredient ingMenu : dailyMenu.getReceta().getIngredientes()) {
            String nombreIngMenu = ingMenu.getNombre().toLowerCase();
            String unidadIngMenu = ingMenu.getUnidad().toLowerCase();
            double cantidadMenu = ingMenu.getCantidad();

            Optional<Ingredient> inventarioMatch = planner.getInventario().stream()
                    .filter(i -> i.getNombre().equalsIgnoreCase(nombreIngMenu) && i.getUnidad().equalsIgnoreCase(unidadIngMenu))
                    .findFirst();

            if (inventarioMatch.isPresent()) {
                double cantidadInventario = inventarioMatch.get().getCantidad();
                double falta = cantidadMenu - cantidadInventario;
                if (falta > 0) {
                    System.out.println("  " + ingMenu.getNombre() + ": Tienes " + (cantidadInventario % 1 == 0 ? (int)cantidadInventario : String.format("%.1f", cantidadInventario)) + ". Te faltan " + (falta % 1 == 0 ? (int)falta : String.format("%.1f", falta)) + ".");
                } else {
                    System.out.println("  " + ingMenu.getNombre() + ": Tienes suficiente (" + (cantidadInventario % 1 == 0 ? (int)cantidadInventario : String.format("%.1f", cantidadInventario)) + ").");
                }
            } else {
                System.out.println("  " + ingMenu.getNombre() + ": No tienes. Necesitas " + (cantidadMenu % 1 == 0 ? (int)cantidadMenu : String.format("%.1f", cantidadMenu)) + ".");
            }
        }
    }

    private static void verInventarioActual() {
        if (planner.getInventario().isEmpty()) {
            System.out.println("El inventario está vacío.");
            return;
        }
        System.out.println("\n--- Inventario Actual ---");
        for (Ingredient ing : planner.getInventario()) {
            System.out.println("- " + ing); // Ingredient's toString handles simplified display
        }
    }

    private static void generarYMostrarListaCompras() {
        System.out.println("\n--- Generando Lista de Compras ---");
        List<Ingredient> listaCompras = planner.calcularListaDeCompras();

        if (listaCompras.isEmpty()) {
            System.out.println("¡No necesitas comprar nada! Tienes todo lo necesario en el inventario.");
            return;
        }

        System.out.println("\n--- Tu Lista de Compras Consolidada ---");
        for (Ingredient ing : listaCompras) {
            System.out.println("- " + ing); // Ingredient's toString handles simplified display
        }

        System.out.println("\n--- Desglose de Ingredientes Faltantes por Receta (si aplica) ---");
        for (DailyMenu dm : planner.getMenuSemanal()) {
            List<Ingredient> missingForRecipe = new ArrayList<>();
            for (Ingredient ingMenu : dm.getReceta().getIngredientes()) {
                String nombreIngMenu = ingMenu.getNombre().toLowerCase();
                String unidadIngMenu = ingMenu.getUnidad().toLowerCase();
                double cantidadMenu = ingMenu.getCantidad();

                Optional<Ingredient> inventarioMatch = planner.getInventario().stream()
                        .filter(i -> i.getNombre().equalsIgnoreCase(nombreIngMenu) && i.getUnidad().equalsIgnoreCase(unidadIngMenu))
                        .findFirst();

                if (inventarioMatch.isPresent()) {
                    double cantidadInventario = inventarioMatch.get().getCantidad();
                    double falta = cantidadMenu - cantidadInventario;
                    if (falta > 0) {
                        missingForRecipe.add(new Ingredient(ingMenu.getNombre(), falta, ingMenu.getUnidad()));
                    }
                } else {
                    missingForRecipe.add(new Ingredient(ingMenu.getNombre(), cantidadMenu, ingMenu.getUnidad()));
                }
            }

            if (!missingForRecipe.isEmpty()) {
                System.out.println("\n-- Para " + dm.getDia() + " - " + dm.getReceta().getNombre() + " (Faltantes): --");
                for (Ingredient missingIng : missingForRecipe) {
                    System.out.println("  - " + missingIng); // Ingredient's toString handles simplified display
                }
            }
        }
    }

    private static void agregarRecetaAMenu() {
        System.out.print("Día de la semana (ej. lunes): ");
        String dia = scanner.nextLine();

        System.out.print("Nombre de la receta: ");
        String nombreReceta = scanner.nextLine();

        System.out.print("¿Cuántos ingredientes tiene esta receta? ");
        int numIngredientes = obtenerOpcionUsuario(); // Reusing the integer input method

        List<Ingredient> ingredientesReceta = new ArrayList<>();
        System.out.println("Ahora ingresa los detalles de cada ingrediente:");

        for (int i = 0; i < numIngredientes; i++) {
            System.out.println("\n--- Ingrediente " + (i + 1) + " ---");
            System.out.print("  Nombre del ingrediente: ");
            String nombreIngrediente = scanner.nextLine();

            System.out.print("  Cantidad (ej. 1, 0.5, 200): ");
            while (!scanner.hasNextDouble()) {
                System.out.println("  Entrada no válida. Por favor, ingresa un número para la cantidad.");
                scanner.next();
            }
            double cantidad = scanner.nextDouble();
            scanner.nextLine(); // Consumir el salto de línea

            System.out.print("  Unidad (ej. piezas, gramos, ml): ");
            String unidad = scanner.nextLine();

            ingredientesReceta.add(new Ingredient(nombreIngrediente, cantidad, unidad));
        }
        planner.addDailyMenu(new DailyMenu(dia, new Recipe(nombreReceta, ingredientesReceta)));
        System.out.println("Receta agregada al menú semanal.");
    }

    private static void agregarItemAInventario() {
        System.out.print("Nombre del ingrediente a agregar al inventario: ");
        String nombre = scanner.nextLine();

        System.out.print("Cantidad: ");
        while (!scanner.hasNextDouble()) {
            System.out.println("Entrada no válida. Por favor, ingresa un número para la cantidad.");
            scanner.next();
        }
        double cantidad = scanner.nextDouble();
        scanner.nextLine(); // Consumir el salto de línea

        System.out.print("Unidad (ej. piezas, gramos, ml): ");
        String unidad = scanner.nextLine();

        planner.addInventarioItem(new Ingredient(nombre, cantidad, unidad));
        System.out.println("Item agregado/actualizado en el inventario.");
    }

    // Método para cargar datos de ejemplo al iniciar la aplicación
    private static void cargarDatosEjemplo() {
        // Inventario
        planner.addInventarioItem(new Ingredient("tomate", 1, "pieza"));
        planner.addInventarioItem(new Ingredient("pasta", 100, "gramos"));
        planner.addInventarioItem(new Ingredient("cebolla", 2, "pieza"));
        planner.addInventarioItem(new Ingredient("lechuga", 0.5, "pieza")); // Inventario parcial para ejemplo

        // Menú Semanal
        List<Ingredient> ensaladaIng = Arrays.asList(
                new Ingredient("lechuga", 1, "pieza"),
                new Ingredient("tomate", 2, "pieza"),
                new Ingredient("pepino", 1, "pieza")
        );
        planner.addDailyMenu(new DailyMenu("lunes", new Recipe("ensalada", ensaladaIng)));

        List<Ingredient> pastaIng = Arrays.asList(
                new Ingredient("pasta", 200, "gramos"),
                new Ingredient("tomate", 1, "pieza"),
                new Ingredient("cebolla", 1, "pieza")
        );
        planner.addDailyMenu(new DailyMenu("martes", new Recipe("pasta", pastaIng)));

        List<Ingredient> polloConArrozIng = Arrays.asList(
                new Ingredient("pollo", 500, "gramos"),
                new Ingredient("arroz", 300, "gramos"),
                new Ingredient("zanahoria", 2, "pieza"),
                new Ingredient("cebolla", 1, "pieza")
        );
        planner.addDailyMenu(new DailyMenu("miércoles", new Recipe("pollo con arroz", polloConArrozIng)));

        System.out.println("¡Datos de ejemplo cargados!");
    }
}

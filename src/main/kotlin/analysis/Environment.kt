package analysis

public class Environment(
    private val variables: MutableMap<String, Type>,
    private val classes: MutableMap<String, ClassType>,
    private val enclosing: Environment? = null,
) {

}
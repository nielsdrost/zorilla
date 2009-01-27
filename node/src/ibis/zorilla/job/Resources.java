package ibis.zorilla.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * specification of a set of resources. Could be used to specify needed
 * resources, avalable resources, etc.
 */
public final class Resources implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int node;
    
    private final int cores;

    private final int memory; // MB

    private final int diskSpace; // MB

    public Resources() {
        this.node = 0;
        this.cores = 0;
        this.memory = 0;
        this.diskSpace = 0;
    }

    public Resources(int node, int cores, int memory, int diskspace) {
        this.node = node;
        this.cores = cores;
        this.memory = memory;
        this.diskSpace = diskspace;
    }

    
    public Resources(JobAttributes attributes) {
        if (attributes.getBooleanProperty(JobAttributes.SCHEDULE_NODES)) {
            node = 1;
        } else {
            node = 0;
        }
        cores = attributes.getCoresPerProcess();
        memory = attributes.getIntProperty(JobAttributes.MEMORY_MAX);
        diskSpace = attributes.getIntProperty(JobAttributes.DISK_SPACE);
    }
    
    public Resources(Resources original) {
        this.node = original.node;
        this.cores = original.cores;
        this.memory = original.memory;
        this.diskSpace = original.diskSpace;
    }

    public Resources subtract(Resources other) {
        return new Resources(node - other.node,cores - other.cores, memory - other.memory, diskSpace - other.diskSpace);
    }

    public Resources add(Resources other) {
        return new Resources(node + other.node,cores + other.cores,
            memory + other.memory, diskSpace + other.diskSpace);
    }

    public Resources mult(int factor) {
        return new Resources(node * factor, cores * factor, memory
            * factor, diskSpace * factor);
    }

    public boolean zero() {
        return node == 0 && cores == 0 && memory == 0 & diskSpace == 0;
    }

    public boolean greaterOrEqualZero() {
        return node >= 0 && cores >= 0 && memory >= 0 && diskSpace >= 0;
    }
    
    public boolean negative() {
        return !greaterOrEqualZero();
    }

    public String toString() {
        return "resources: node = " + node + ", cores = " + cores
            + ", memory=" + memory + ", diskSpace=" + diskSpace;
    }
    
    public Map<String, String> asStringMap() {
        Map<String, String> result = new HashMap<String, String>();

        result.put("node", Integer.toString(node));
        result.put("cores", Integer.toString(cores));
        result.put("memory", Integer.toString(memory));
        result.put("disk.space", Integer.toString(diskSpace));
        
        return result;
        
    }

}

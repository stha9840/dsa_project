import java.util.*;

public class HouseFriendship {

    // Union-Find class to manage connected components
    static class UnionFind {
        int[] parent, rank;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // path compression
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX != rootY) {
                if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }
    }

    public static List<String> friendRequests(int n, int[][] restrictions, int[][] requests) {
        UnionFind uf = new UnionFind(n);
        List<String> result = new ArrayList<>();

        for (int[] request : requests) {
            int houseA = request[0];
            int houseB = request[1];

            boolean canBeFriends = true;
            for (int[] restriction : restrictions) {
                int restrictedA = restriction[0];
                int restrictedB = restriction[1];

                // Check if making houseA and houseB friends violates any restriction
                if (uf.connected(houseA, restrictedA) && uf.connected(houseB, restrictedB) ||
                        uf.connected(houseA, restrictedB) && uf.connected(houseB, restrictedA)) {
                    canBeFriends = false;
                    break;
                }
            }

            if (canBeFriends) {
                uf.union(houseA, houseB);
                result.add("approved");
            } else {
                result.add("denied");
            }
        }

        return result;
    }

    public static void main(String[] args) {
        int n1 = 3;
        int[][] restrictions1 = {{0, 1}};
        int[][] requests1 = {{0, 2}, {2, 1}};
        System.out.println(friendRequests(n1, restrictions1, requests1)); // Output: [approved, denied]

        int n2 = 5;
        int[][] restrictions2 = {{0, 1}, {1, 2}, {2, 3}};
        int[][] requests2 = {{0, 4}, {1, 2}, {3, 1}, {3, 4}};
        System.out.println(friendRequests(n2, restrictions2, requests2)); // Output: [approved, denied, approved, denied]
    }
}

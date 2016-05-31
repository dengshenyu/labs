import java.util.*;

/**
 * This is just an implementation of Delaunay Triangulation in Java, which was mostly ported from
 * <a href="https://github.com/ironwallaby/delaunay">ironwallaby's javascript implementation</a>.
 *
 * @author Deng Shenyuan
 */
public class Delaunay {
    private static double EPSILON = 1.0 / 1048576.0;

    public static double[][] supertriangle(double[][] vertices) {
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = -Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;

        for (int i = vertices.length; i-- > 0; ) {
            if (vertices[i][0] < xmin) xmin = vertices[i][0];
            if (vertices[i][0] > xmax) xmax = vertices[i][0];
            if (vertices[i][1] < ymin) ymin = vertices[i][1];
            if (vertices[i][1] > ymax) ymax = vertices[i][1];
        }

        double dx = xmax - xmin;
        double dy = ymax - ymin;
        double dmax = Math.max(dx, dy);
        double xmid = xmin + dx * 0.5;
        double ymid = ymin + dy * 0.5;

        return new double[][]{
                {xmid - 20 * dmax, ymid - dmax},
                {xmid, ymid + 20 * dmax},
                {xmid + 20 * dmax, ymid - dmax}
        };
    }

    public static CircumCircle circumcircle(double[][] vertices, int i, int j, int k) throws Exception{
        double x1 = vertices[i][0],
                y1 = vertices[i][1],
                x2 = vertices[j][0],
                y2 = vertices[j][1],
                x3 = vertices[k][0],
                y3 = vertices[k][1],
                fabsy1y2 = Math.abs(y1 - y2),
                fabsy2y3 = Math.abs(y2 - y3);
        /* Check for coincident points */
        if (fabsy1y2 < EPSILON && fabsy2y3 < EPSILON)
            throw new Exception("Eek! Coincident points!");

        double m1, m2, mx1, mx2, my1, my2, xc, yc, dx, dy;
        if (fabsy1y2 < EPSILON) {
            m2  = -((x3 - x2) / (y3 - y2));
            mx2 = (x2 + x3) / 2.0;
            my2 = (y2 + y3) / 2.0;
            xc  = (x2 + x1) / 2.0;
            yc  = m2 * (xc - mx2) + my2;
        } else if (fabsy2y3 < EPSILON) {
            m1  = -((x2 - x1) / (y2 - y1));
            mx1 = (x1 + x2) / 2.0;
            my1 = (y1 + y2) / 2.0;
            xc  = (x3 + x2) / 2.0;
            yc  = m1 * (xc - mx1) + my1;
        } else {
            m1  = -((x2 - x1) / (y2 - y1));
            m2  = -((x3 - x2) / (y3 - y2));
            mx1 = (x1 + x2) / 2.0;
            mx2 = (x2 + x3) / 2.0;
            my1 = (y1 + y2) / 2.0;
            my2 = (y2 + y3) / 2.0;
            xc  = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            yc  = (fabsy1y2 > fabsy2y3) ?
                    m1 * (xc - mx1) + my1 :
                    m2 * (xc - mx2) + my2;
        }

        dx = x2 - xc;
        dy = y2 - yc;
        return new CircumCircle(i, j, k, xc, yc, dx * dx + dy * dy);
    }

    public static List<Integer> dedup(List<Integer> edges) {

        for (int j = edges.size(); j > 0; ) {
            if (j > edges.size())
                j = edges.size();
            Integer b = edges.get(--j);
            Integer a = edges.get(--j);

            for (int i = j; i > 0; ) {
                Integer n = edges.get(--i);
                Integer m = edges.get(--i);

                if ((a.equals(m) && b.equals(n)) || (a.equals(n) && b.equals(m))) {
                    edges = splice(edges, j, 2);
                    edges = splice(edges, i ,2);
                    break;
                }
            }
        }

        return edges;
    }

    public static List<Integer> triangulate(double[][] vertices) throws Exception{
        int n = vertices.length;

        /* Bail if there aren't enough vertices to form any triangles. */
        if(n < 3)
            return Collections.EMPTY_LIST;

        /* Make an array of indices into the vertex array, sorted by the
       * vertices' x-position. */
        Integer[] indices = new Integer[n];

        for(int i = n; i-- > 0; )
            indices[i] = i;

        final double[][] ftv = new double[vertices.length][];
        for (int i = 0; i < vertices.length; i++)
            ftv[i] = vertices[i].clone();
        Arrays.sort(indices, new Comparator<Integer>(){
            public int compare(Integer i, Integer j) {
                return (ftv[j][0] - ftv[i][0] < 0) ? -1 : (ftv[j][0] - ftv[i][0] == 0) ? 0 : 1;
            }
        });

        /* Next, find the vertices of the supertriangle (which contains all other
         * triangles), and append them onto the end of a (copy of) the vertex
         * array. */
        double[][] tv = new double[vertices.length + 3][];
        for (int i = 0; i < vertices.length; i++)
            tv[i] = vertices[i].clone();
        double[][] st = supertriangle(vertices);
        tv[vertices.length] = st[0];
        tv[vertices.length + 1] = st[1];
        tv[vertices.length + 2] = st[2];
        vertices = tv;

        List<CircumCircle> open = new ArrayList<CircumCircle>();
        open.add(circumcircle(vertices, n + 0, n + 1, n + 2));

        List<CircumCircle> closed = new ArrayList<CircumCircle>();
        List<Integer> edges = new ArrayList<Integer>();

        for (int i = indices.length; i-- > 0; edges = new ArrayList<Integer>()) {
            int c = indices[i];

            /* For each open triangle, check to see if the current point is
             * inside it's circumcircle. If it is, remove the triangle and add
             * it's edges to an edge list. */
            for (int j = open.size(); j-- > 0; ) {
                /* If this point is to the right of this triangle's circumcircle,
                 * then this triangle should never get checked again. Remove it
                 * from the open list, add it to the closed list, and skip. */
                double dx =vertices[c][0] - open.get(j).x;
                if(dx > 0.0 && dx * dx > open.get(j).r) {
                    closed.add(open.get(j));
                    open = splice(open, j, 1);
                    continue;
                }

                /* If we're outside the circumcircle, skip this triangle. */
                double dy = vertices[c][1] - open.get(j).y;
                if(dx * dx + dy * dy - open.get(j).r > EPSILON)
                    continue;

                /* Remove the triangle and add it's edges to the edge list. */
                edges.add(open.get(j).i);
                edges.add(open.get(j).j);
                edges.add(open.get(j).j);
                edges.add(open.get(j).k);
                edges.add(open.get(j).k);
                edges.add(open.get(j).i);

                open = splice(open, j, 1);
            }

            /* Remove any doubled edges. */
            edges = dedup(edges);

            /* Add a new triangle for each edge. */
            for(int j = edges.size(); j > 0; ) {
                Integer b = edges.get(--j);
                Integer a = edges.get(--j);
                open.add(circumcircle(vertices, a, b, c));
            }
        }

        /* Copy any remaining open triangles to the closed list, and then
         * remove any triangles that share a vertex with the supertriangle,
         * building a list of triplets that represent triangles. */
        for(int i = open.size(); i-- > 0; )
            closed.add(open.get(i));


        List<Integer> result = new ArrayList<Integer>();

        for(int i = closed.size(); i-- > 0; )
            if(closed.get(i).i < n && closed.get(i).j < n && closed.get(i).k < n) {
                result.add(closed.get(i).i);
                result.add(closed.get(i).j);
                result.add(closed.get(i).k);
            }

        /* Yay, we're done! */
        return result;

    }

    static <T> List<T> splice(List<T> orig, int start, int len) {
        List<T> re = new ArrayList<T>(orig.subList(0, start));
        re.addAll(orig.subList(start + len, orig.size()));
        return re;
    }


    static class CircumCircle {
        int i, j, k;
        double x, y, r;

        CircumCircle(int i, int j, int k, double x, double y, double r) {
            this.i = i; this.j = j; this.k = k;
            this.x = x; this.y = y; this.r = r;
        }
    }


}

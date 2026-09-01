import java.io.PrintWriter;
import java.util.*;
public class PruebaConversor {
    static int fallos=0;
    static void debe(String q,boolean c){System.out.println((c?"  [OK]   ":"  [FALLA]")+" "+q); if(!c)fallos++;}

    static Entidad ent(ModeloER m,String n,int x,int y,boolean debil){
        Entidad e=new Entidad(n,new Punto(x,y),debil); m.agregarEntidad(e); return e; }
    static Atributo att(Entidad e,String n,TipoDato t,Naturaleza nat,Marca... ms){
        Set<Marca> s=EnumSet.noneOf(Marca.class); s.addAll(Arrays.asList(ms));
        Atributo a=new Atributo(n,t,nat,s,new Punto(0,-110));
        a.setDesplazamiento(new Punto(0,-110)); e.agregarAtributo(a); return a; }
    static Relacion rel(ModeloER m,String n,int x,int y,boolean ident){
        Relacion r=new Relacion(n,new Punto(x,y),ident); m.agregarRelacion(r); return r; }
    static void une(Relacion r,Entidad e,Cardinalidad c,Modalidad mo){
        r.agregarParticipacion(new Participacion(e.getId(),c,mo,"")); }

    static String ddl(EsquemaRelacional e){ return new GeneradorSQL(Destino.SQLITE).generar(e); }

    static boolean sqliteAcepta(EsquemaRelacional e,String archivo) throws Exception {
        try(PrintWriter w=new PrintWriter("/tmp/aud_"+archivo+".sql")){ w.print(ddl(e)); }
        Process p=new ProcessBuilder("sh","-c",
            "rm -f /tmp/aud_"+archivo+".db && sqlite3 /tmp/aud_"+archivo+".db < /tmp/aud_"+archivo+".sql 2>&1")
            .redirectErrorStream(true).start();
        String salida=new String(p.getInputStream().readAllBytes());
        p.waitFor();
        if(!salida.isBlank()) System.out.println("        sqlite dijo: "+salida.trim().lines().findFirst().orElse(""));
        return salida.isBlank();
    }

    public static void main(String[] a) throws Exception {
        System.out.println("=== 1. relacion 1:1 ===");
        ModeloER m=new ModeloER();
        Entidad p1=ent(m,"Persona",100,100,false); att(p1,"dni",TipoDato.TEXTO_CORTO,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad c1=ent(m,"Carnet",400,100,false); att(c1,"numero",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion r1=rel(m,"Posee",250,100,false);
        une(r1,p1,Cardinalidad.UNO,Modalidad.PARCIAL); une(r1,c1,Cardinalidad.UNO,Modalidad.TOTAL);
        EsquemaRelacional e1=new Conversor().convertir(m).getEsquema();
        debe("1:1 genera 2 tablas", e1.getTablas().size()==2);
        debe("1:1 el DDL se ejecuta", sqliteAcepta(e1,"uno"));
        int fks=0; for(Tabla t:e1.getTablas()) fks+=t.foraneas().size();
        debe("1:1 crea exactamente una foranea", fks==1);

        System.out.println("=== 2. relacion n-aria (3 entidades) ===");
        m=new ModeloER();
        Entidad x=ent(m,"Profesor",100,100,false); att(x,"idp",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad y=ent(m,"Curso",400,100,false); att(y,"idc",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad z=ent(m,"Aula",700,100,false); att(z,"ida",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion rn=rel(m,"Imparte",400,300,false);
        une(rn,x,Cardinalidad.MUCHOS,Modalidad.PARCIAL); une(rn,y,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        une(rn,z,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        EsquemaRelacional e2=new Conversor().convertir(m).getEsquema();
        debe("n-aria genera 4 tablas", e2.getTablas().size()==4);
        Tabla ti=e2.buscarTabla("imparte");
        debe("la tabla n-aria tiene 3 foraneas", ti!=null && ti.foraneas().size()==3);
        debe("y clave compuesta de 3", ti!=null && ti.primaria()!=null && ti.primaria().getColumnas().size()==3);
        debe("n-aria el DDL se ejecuta", sqliteAcepta(e2,"naria"));

        System.out.println("=== 3. relacion recursiva ===");
        m=new ModeloER();
        Entidad em=ent(m,"Empleado",100,100,false); att(em,"ide",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion rr=rel(m,"Supervisa",400,100,false);
        rr.agregarParticipacion(new Participacion(em.getId(),Cardinalidad.UNO,Modalidad.PARCIAL,"jefe"));
        rr.agregarParticipacion(new Participacion(em.getId(),Cardinalidad.MUCHOS,Modalidad.PARCIAL,"subordinado"));
        try {
            EsquemaRelacional e3=new Conversor().convertir(m).getEsquema();
            debe("recursiva no revienta", true);
            debe("recursiva el DDL se ejecuta", sqliteAcepta(e3,"rec"));
        } catch(Exception ex){ debe("recursiva no revienta: "+ex.getClass().getSimpleName()+" "+ex.getMessage(), false); }

        System.out.println("=== 4. N:M con atributos propios ===");
        m=new ModeloER();
        Entidad a1=ent(m,"Alumno",100,100,false); att(a1,"ida",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad m1=ent(m,"Materia",400,100,false); att(m1,"idm",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion rm=rel(m,"Cursa",250,100,false);
        une(rm,a1,Cardinalidad.MUCHOS,Modalidad.PARCIAL); une(rm,m1,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        EsquemaRelacional e4=new Conversor().convertir(m).getEsquema();
        Tabla tc=e4.buscarTabla("cursa");
        debe("N:M crea la tabla intermedia", tc!=null);
        debe("N:M el DDL se ejecuta", sqliteAcepta(e4,"nm"));

        System.out.println("=== 5. entidad debil ===");
        m=new ModeloER();
        Entidad pr=ent(m,"Profesor",100,100,false); att(pr,"idp",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad fa=ent(m,"Familiar",400,100,true); att(fa,"nombre",TipoDato.TEXTO_MEDIO,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion rt=rel(m,"Tiene",250,100,true);
        une(rt,pr,Cardinalidad.UNO,Modalidad.PARCIAL); une(rt,fa,Cardinalidad.MUCHOS,Modalidad.TOTAL);
        EsquemaRelacional e5=new Conversor().convertir(m).getEsquema();
        Tabla tf=e5.buscarTabla("familiar");
        debe("la debil tiene 2 columnas y no 3", tf!=null && tf.getColumnas().size()==2);
        debe("una sola foranea", tf!=null && tf.foraneas().size()==1);
        debe("debil el DDL se ejecuta", sqliteAcepta(e5,"debil"));

        System.out.println("=== 6. modelo vacio y entidad suelta ===");
        EsquemaRelacional e6=new Conversor().convertir(new ModeloER()).getEsquema();
        debe("modelo vacio no revienta", e6.getTablas().isEmpty());
        m=new ModeloER(); Entidad sola=ent(m,"Sola",0,0,false);
        att(sola,"id",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        debe("entidad suelta se convierte",
            new Conversor().convertir(m).getEsquema().getTablas().size()==1);

        System.out.println("=== 7. las foraneas parciales admiten nulos ===");
        m=new ModeloER();
        Entidad e7=ent(m,"Empleado",0,0,false); att(e7,"ide",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad d7=ent(m,"Departamento",300,0,false); att(d7,"idd",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion r7=rel(m,"Pertenece",150,0,false);
        une(r7,d7,Cardinalidad.UNO,Modalidad.PARCIAL); une(r7,e7,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        Tabla te=new Conversor().convertir(m).getEsquema().buscarTabla("empleado");
        Columna fk=null;
        for(Columna c:te.getColumnas()) if(te.esForanea(c.getNombre())) fk=c;
        debe("participacion parcial: la foranea admite nulos", fk!=null && fk.admiteNulos());
        m=new ModeloER();
        Entidad e8=ent(m,"Empleado",0,0,false); att(e8,"ide",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad d8=ent(m,"Departamento",300,0,false); att(d8,"idd",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion r8=rel(m,"Pertenece",150,0,false);
        une(r8,d8,Cardinalidad.UNO,Modalidad.PARCIAL); une(r8,e8,Cardinalidad.MUCHOS,Modalidad.TOTAL);
        Tabla te2=new Conversor().convertir(m).getEsquema().buscarTabla("empleado");
        Columna fk2=null;
        for(Columna c:te2.getColumnas()) if(te2.esForanea(c.getNombre())) fk2=c;
        debe("participacion total: la foranea es obligatoria", fk2!=null && !fk2.admiteNulos());

        System.out.println("=== 8. las foraneas no heredan SERIAL ===");
        boolean sinSerial=true;
        for(Columna c:te.getColumnas()) if(te.esForanea(c.getNombre()) && c.getTipo()==TipoDato.SERIAL) sinSerial=false;
        debe("la foranea copia el tipo base, no la secuencia", sinSerial);

        System.out.println("=== 9. multivaluados y derivados fuera de la tabla ===");
        m=new ModeloER();
        Entidad e9=ent(m,"Persona",0,0,false);
        att(e9,"id",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        att(e9,"telefono",TipoDato.TEXTO_CORTO,Naturaleza.MULTIVALUADO);
        att(e9,"edad",TipoDato.ENTERO,Naturaleza.DERIVADO);
        EsquemaRelacional e9s=new Conversor().convertir(m).getEsquema();
        Tabla tp=e9s.buscarTabla("persona");
        debe("la entidad solo tiene su columna simple", tp.getColumnas().size()==1);
        debe("el multivaluado tiene su propia tabla", e9s.getTablas().size()==2);
        debe("el derivado no genera nada", e9s.buscarTabla("persona_edad")==null);

        System.out.println("=== 10. validar() detecta un esquema roto ===");
        EsquemaRelacional roto=new EsquemaRelacional();
        Tabla ta=new Tabla("a",OrigenTabla.ENTIDAD_FUERTE,null);
        ta.agregarColumna(new Columna("ida",TipoDato.SERIAL,false));
        ta.definirClave(List.of("ida")); roto.agregarTabla(ta);
        Tabla tb=new Tabla("b",OrigenTabla.ENTIDAD_FUERTE,null);
        tb.agregarColumna(new Columna("x",TipoDato.ENTERO,false));
        tb.definirClave(List.of("x"));
        tb.restringir(Restriccion.foranea(List.of("x"),"a",List.of("no_existe"),
            AccionReferencial.CASCADA,AccionReferencial.CASCADA));
        roto.agregarTabla(tb);
        debe("detecta la columna referenciada inexistente", !roto.validar().isEmpty());

        System.out.println("=== 11. relacion a medio enlazar ===");
        m=new ModeloER();
        Entidad su=ent(m,"Suelta",0,0,false); att(su,"id",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion incompleta=rel(m,"Incompleta",0,0,false);
        une(incompleta,su,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        ResultadoConversion parcial=new Conversor().convertir(m);
        debe("una relacion con una sola participacion avisa", !parcial.getAvisos().isEmpty());
        debe("y no genera una tabla sin sentido",
            parcial.getEsquema().buscarTabla("incompleta")==null);

        System.out.println("=== 12. atributos propios de una relacion ===");
        m=new ModeloER();
        Entidad al2=ent(m,"Alumno",0,0,false); att(al2,"ida",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Entidad ma2=ent(m,"Materia",0,0,false); att(ma2,"idm",TipoDato.SERIAL,Naturaleza.SIMPLE,Marca.CLAVE);
        Relacion cu=rel(m,"Cursa",0,0,false);
        une(cu,al2,Cardinalidad.MUCHOS,Modalidad.PARCIAL); une(cu,ma2,Cardinalidad.MUCHOS,Modalidad.PARCIAL);
        cu.agregarAtributo(new Atributo("nota",TipoDato.DECIMAL,Naturaleza.SIMPLE,
            EnumSet.noneOf(Marca.class),new Punto(0,-110)));
        EsquemaRelacional se=new Conversor().convertir(m).getEsquema();
        Tabla tcu=se.buscarTabla("cursa");
        debe("el atributo de la relacion llega a la tabla intermedia",
            tcu!=null && tcu.buscarColumna("nota")!=null);
        debe("con su tipo", tcu!=null && tcu.buscarColumna("nota").getTipo()==TipoDato.DECIMAL);
        debe("N:M con atributos: el DDL se ejecuta", sqliteAcepta(se,"nota"));

        System.out.println("=== 13. entidad sin clave ===");
        m=new ModeloER(); Entidad sc=ent(m,"SinClave",0,0,false);
        att(sc,"dato",TipoDato.TEXTO_CORTO,Naturaleza.SIMPLE);
        try { ResultadoConversion rc=new Conversor().convertir(m);
              debe("sin clave avisa en vez de reventar", !rc.getAvisos().isEmpty());
        } catch(Exception ex){ debe("sin clave revienta: "+ex.getMessage(), false); }
        System.exit(fallos);
    }
}

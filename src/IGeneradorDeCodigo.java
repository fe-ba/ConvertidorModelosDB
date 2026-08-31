// Contrato de todo generador: recibe el esquema ya convertido y devuelve texto.
public interface IGeneradorDeCodigo {

    Destino getDestino();

    String generar(EsquemaRelacional esquema);
}

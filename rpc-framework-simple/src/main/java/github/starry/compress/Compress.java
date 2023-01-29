package github.starry.compress;


import github.starry.extension.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}

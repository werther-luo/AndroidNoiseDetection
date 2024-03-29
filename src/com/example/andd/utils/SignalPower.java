	

package com.example.andd.utils;


/**
 * A power metering algorithm.
 */
public final class SignalPower {

    private SignalPower() {
    }
    

    
    /**
     * Calculate the power of the given input signal.
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @return              The calculated power in dB relative to the maximum
     *                      input level; hence 0dB represents maximum power,
     *                      and minimum power is about -95dB.  Particular
     *                      cases of interest:
     *                      <ul>
     *                      <li>A non-clipping full-range sine wave input is
     *                          about -2.41dB.
     *                      <li>Saturated input (heavily clipped) approaches
     *                          0dB.
     *                      <li>A low-frequency fully saturated input can
     *                          get above 0dB, but this would be pretty
     *                          artificial.
     *                      <li>A really tiny signal, which only occasionally
     *                          deviates from zero, can get below -100dB.
     *                      <li>A completely zero input will produce an
     *                          output of -Infinity.
     *                      </ul>
     *                      <b>You must be prepared to handle this infinite
     *                      result and results greater than zero,</b> although
     *                      clipping them off would be quite acceptable in
     *                      most cases.
     */
    public final static double calculatePowerDb(short[] sdata, int off, int samples) {
        // Calculate the sum of the values, and the sum of the squared values.
        // We need longs to avoid running out of bits.
        double sum = 0;
        double sqsum = 0;
        for (int i = 0; i < samples; i++) {
            final long v = sdata[off + i];
            sum += v;
            sqsum += v * v;
        }
        
        // sqsum is the sum of all (signal+bias)², so
        //     sqsum = sum(signal²) + samples * bias²
        // hence
        //     sum(signal²) = sqsum - samples * bias²
        // Bias is simply the average value, i.e.
        //     bias = sum / samples
        // Since power = sum(signal²) / samples, we have
        //     power = (sqsum - samples * sum² / samples²) / samples
        // so
        //     power = (sqsum - sum² / samples) / samples
        double power = (sqsum - sum * sum / samples) / samples;

        // Scale to the range 0 - 1.
        power /= MAX_16_BIT * MAX_16_BIT;

        // Convert to dB, with 0 being max power.  Add a fudge factor to make
        // a "real" fully saturated input come to 0 dB.
        return Math.log10(power) * 10f + FUDGE;
    }
    

    // ******************************************************************** //
    // Constants.
    // ******************************************************************** //

    // Maximum signal amplitude for 16-bit data.
    private static final float MAX_16_BIT = 32768;
    
    // This fudge factor is added to the output to make a realistically
    // fully-saturated signal come to 0dB.  Without it, the signal would
    // have to be solid samples of -32768 to read zero, which is not
    // realistic.  This really is a fudge, because the best value depends
    // on the input frequency and sampling rate.  We optimise here for
    // a 1kHz signal at 16,000 samples/sec.
    private static final float FUDGE = 0.6f;
  
}


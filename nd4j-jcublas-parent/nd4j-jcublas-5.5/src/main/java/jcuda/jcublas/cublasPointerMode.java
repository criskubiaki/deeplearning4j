/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */
package jcuda.jcublas;

/**
 * Indicates whether the scalar values are passed by
 * reference on the host or device
 * 
 * @see JCublas2#cublasSetPointerMode(cublasHandle, int)
 */
public class cublasPointerMode
{
    /**
     * The scalars are passed by reference on the host
     */
    public static final int CUBLAS_POINTER_MODE_HOST = 0;

    /**
     * The scalars are passed by reference on the device
     */
    public static final int CUBLAS_POINTER_MODE_DEVICE = 1;

    /**
     * Private constructor to prevent instantiation
     */
    private cublasPointerMode(){}

    /**
     * Returns a string representation of the given constant
     *
     * @return A string representation of the given constant
     */
    public static String stringFor(int n)
    {
        switch (n)
        {
            case CUBLAS_POINTER_MODE_HOST: return "CUBLAS_POINTER_MODE_HOST";
            case CUBLAS_POINTER_MODE_DEVICE: return "CUBLAS_POINTER_MODE_DEVICE";
        }
        return "INVALID cublasPointerMode: "+n;
    }
}


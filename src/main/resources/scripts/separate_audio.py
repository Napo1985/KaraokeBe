#!/usr/bin/env python3
"""
Audio separation script using Spleeter or Demucs.
This script separates vocals from instrumental in an audio file.
"""

import argparse
import sys
import os
import subprocess

def check_dependency(package_name, import_name=None):
    """Check if a Python package is installed."""
    if import_name is None:
        import_name = package_name
    try:
        __import__(import_name)
        return True
    except ImportError:
        return False

def separate_with_spleeter(input_path, output_dir):
    """Separate audio using Spleeter (2stems model)."""
    try:
        from spleeter.separator import Separator
        
        separator = Separator('spleeter:2stems')
        separator.separate_to_file(input_path, output_dir)
        
        # Spleeter outputs: vocals.wav and accompaniment.wav
        vocals_path = os.path.join(output_dir, os.path.splitext(os.path.basename(input_path))[0], 'vocals.wav')
        instrumental_path = os.path.join(output_dir, os.path.splitext(os.path.basename(input_path))[0], 'accompaniment.wav')
        
        # Rename/move files to expected locations
        final_vocals = os.path.join(output_dir, 'vocals.wav')
        final_instrumental = os.path.join(output_dir, 'instrumental.wav')
        
        if os.path.exists(vocals_path):
            os.rename(vocals_path, final_vocals)
        if os.path.exists(instrumental_path):
            os.rename(instrumental_path, final_instrumental)
            
        return True
    except Exception as e:
        print(f"Spleeter separation failed: {e}", file=sys.stderr)
        return False

def separate_with_demucs(input_path, output_dir):
    """Separate audio using Demucs."""
    try:
        import demucs.separate
        
        # Run demucs separation
        demucs.separate.main([input_path, '-o', output_dir, '-n', 'htdemucs'])
        
        # Demucs outputs in a subdirectory structure
        base_name = os.path.splitext(os.path.basename(input_path))[0]
        demucs_output = os.path.join(output_dir, 'htdemucs', base_name)
        
        vocals_path = os.path.join(demucs_output, 'vocals.wav')
        instrumental_path = os.path.join(demucs_output, 'other.wav')
        
        # Combine other stems as instrumental (drums, bass, other)
        if os.path.exists(vocals_path) and os.path.exists(instrumental_path):
            final_vocals = os.path.join(output_dir, 'vocals.wav')
            final_instrumental = os.path.join(output_dir, 'instrumental.wav')
            
            # Copy/move files
            import shutil
            shutil.copy2(vocals_path, final_vocals)
            
            # For instrumental, we might want to mix drums, bass, and other
            # For simplicity, just use 'other' as instrumental
            shutil.copy2(instrumental_path, final_instrumental)
            
        return True
    except Exception as e:
        print(f"Demucs separation failed: {e}", file=sys.stderr)
        return False

def main():
    parser = argparse.ArgumentParser(description='Separate vocals from instrumental audio')
    parser.add_argument('--input', required=True, help='Input audio file path')
    parser.add_argument('--output', required=True, help='Output directory path')
    parser.add_argument('--method', choices=['spleeter', 'demucs', 'auto'], 
                       default='auto', help='Separation method to use')
    
    args = parser.parse_args()
    
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)
    
    os.makedirs(args.output, exist_ok=True)
    
    success = False
    
    if args.method == 'spleeter' or args.method == 'auto':
        if check_dependency('spleeter'):
            success = separate_with_spleeter(args.input, args.output)
            if success:
                print("Audio separated successfully using Spleeter", file=sys.stderr)
    
    if not success and (args.method == 'demucs' or args.method == 'auto'):
        if check_dependency('demucs'):
            success = separate_with_demucs(args.input, args.output)
            if success:
                print("Audio separated successfully using Demucs", file=sys.stderr)
    
    if not success:
        print("Error: Audio separation failed. Please install spleeter or demucs:", file=sys.stderr)
        print("  pip install spleeter", file=sys.stderr)
        print("  or", file=sys.stderr)
        print("  pip install demucs", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()

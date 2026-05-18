let sharedAudioElement = null
let sharedAudioContext = null
let sharedSourceNode = null
let sharedGainNode = null
let sharedAnalyserNode = null

function createAudioContext() {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) {
        return null
    }
    return new AudioContextClass()
}

export function ensureAudioGraph(audioElement) {
    if (!audioElement) {
        return null
    }

    if (sharedAudioElement === audioElement && sharedAudioContext && sharedGainNode && sharedAnalyserNode) {
        return {
            audioContext: sharedAudioContext,
            sourceNode: sharedSourceNode,
            gainNode: sharedGainNode,
            analyserNode: sharedAnalyserNode
        }
    }

    if (sharedSourceNode || sharedGainNode || sharedAnalyserNode) {
        try {
            sharedSourceNode?.disconnect()
            sharedGainNode?.disconnect()
            sharedAnalyserNode?.disconnect()
        } catch (error) {
            console.warn('清理旧音频图失败', error)
        }
    }

    sharedAudioContext = createAudioContext()
    if (!sharedAudioContext) {
        return null
    }

    sharedAudioElement = audioElement
    sharedSourceNode = sharedAudioContext.createMediaElementSource(audioElement)
    sharedGainNode = sharedAudioContext.createGain()
    sharedAnalyserNode = sharedAudioContext.createAnalyser()
    sharedAnalyserNode.fftSize = 1024

    sharedSourceNode.connect(sharedGainNode)
    sharedGainNode.connect(sharedAnalyserNode)
    sharedAnalyserNode.connect(sharedAudioContext.destination)

    return {
        audioContext: sharedAudioContext,
        sourceNode: sharedSourceNode,
        gainNode: sharedGainNode,
        analyserNode: sharedAnalyserNode
    }
}

export function getAudioGraph() {
    if (!sharedAudioContext || !sharedGainNode || !sharedAnalyserNode) {
        return null
    }

    return {
        audioContext: sharedAudioContext,
        sourceNode: sharedSourceNode,
        gainNode: sharedGainNode,
        analyserNode: sharedAnalyserNode,
        audioElement: sharedAudioElement
    }
}

export function setMasterGain(value) {
    if (!sharedGainNode) {
        return
    }
    sharedGainNode.gain.value = value
}

export function resumeAudioGraph() {
    if (sharedAudioContext?.state === 'suspended') {
        return sharedAudioContext.resume().catch(() => { })
    }
    return Promise.resolve()
}

export function teardownAudioGraph(audioElement) {
    if (audioElement && sharedAudioElement && sharedAudioElement !== audioElement) {
        return
    }

    try {
        sharedSourceNode?.disconnect()
        sharedGainNode?.disconnect()
        sharedAnalyserNode?.disconnect()
    } catch (error) {
        console.warn('断开音频图失败', error)
    }

    try {
        sharedAudioContext?.close()
    } catch (error) {
        console.warn('关闭音频上下文失败', error)
    }

    sharedAudioElement = null
    sharedAudioContext = null
    sharedSourceNode = null
    sharedGainNode = null
    sharedAnalyserNode = null
}
import mitt from 'mitt'

export const eventBus = mitt()

export function requestConfirm(options = {}) {
    return new Promise((resolve) => {
        eventBus.emit('request-confirm', {
            ...options,
            onConfirm: () => resolve(true),
            onCancel: () => resolve(false)
        })
    })
}

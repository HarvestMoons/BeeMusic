import { ref } from 'vue'
import { API_BASE } from '@/constants'

export function useStationMaster(playlist, currentIndex, showToastMessage) {
    const showConfirmModal = ref(false)
    const confirmModalTitle = ref('')
    const confirmModalMessage = ref('')
    const pendingAction = ref(null)

    async function handleToggleDeleteStatus() {
        const song = playlist.value[currentIndex.value];
        if (!song) return;

        const isDeleted = song.isDeleted === 1;
        const action = isDeleted ? '恢复' : '删除';

        confirmModalTitle.value = `${action}歌曲`
        confirmModalMessage.value = `确定要${action}当前播放的歌曲 "${song.name}" 吗？`
        pendingAction.value = { song, isDeleted, action }
        showConfirmModal.value = true
    }

    async function executeDeleteOrRestore() {
        if (!pendingAction.value) return

        const { song, isDeleted, action } = pendingAction.value

        try {
            const endpoint = isDeleted ? 'restore' : 'delete';
            const res = await fetch(`${API_BASE}/songs/${endpoint}/${song.id}`, {
                method: 'POST',
                credentials: 'include'
            });

            if (res.ok) {
                showToastMessage(`歌曲已${action}`);
                song.isDeleted = isDeleted ? 0 : 1;
            } else {
                showToastMessage(`${action}失败`);
            }
        } catch (e) {
            console.error(e);
            showToastMessage('操作异常');
        } finally {
            pendingAction.value = null
        }
    }

    return {
        showConfirmModal,
        confirmModalTitle,
        confirmModalMessage,
        handleToggleDeleteStatus,
        executeDeleteOrRestore
    }
}

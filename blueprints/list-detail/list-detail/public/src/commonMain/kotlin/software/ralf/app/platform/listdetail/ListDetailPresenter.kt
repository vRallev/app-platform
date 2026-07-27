package software.ralf.app.platform.listdetail

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Root presenter contract for the feature.
 *
 * The implementation returns [BaseModel] because the concrete phone and tablet models differ and
 * are selected adaptively at runtime.
 */
interface ListDetailPresenter : MoleculePresenter<Unit, BaseModel>
